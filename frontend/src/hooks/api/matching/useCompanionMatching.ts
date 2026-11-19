import { useEffect, useRef, useState } from "react";
import type { CandidateQuery, CandidateResult } from "@/hooks/matching.types";
import { ApiError } from "@/api/apiError";
import { api } from "@/api/api";
import type { components } from "@/api/generated/schema";

export const ACTIVE_RECRUITMENT_LIMIT = 3;

type MatchingErrorKey =
    | "matchingFlow.saveError"
    | "matchingFlow.activeLimit"
    | "matchingFlow.travelStyleRequired"
    | "matchingFlow.resumeDestinationRequired"
    | "matchingFlow.resumeDestinationUnavailable"
    | "matchingFlow.catalogError";

const errorKeys: Readonly<Record<string, MatchingErrorKey>> = {
    COMPANION_SEARCH_LIMIT_EXCEEDED: "matchingFlow.activeLimit",
    TRAVEL_STYLE_REQUIRED: "matchingFlow.travelStyleRequired",
    COMPANION_SEARCH_DESTINATION_REQUIRED: "matchingFlow.resumeDestinationRequired",
    DESTINATION_NOT_FOUND: "matchingFlow.resumeDestinationUnavailable",
    DESTINATION_UNAVAILABLE: "matchingFlow.resumeDestinationUnavailable",
    DESTINATION_INACTIVE: "matchingFlow.resumeDestinationUnavailable",
};

type Schemas = components["schemas"];
export type MatchingConditions = Schemas["SaveCompanionSearchRequest"];

export function useCompanionMatching(userId: number | undefined) {
    const [destinations, setDestinations] = useState<Schemas["DestinationSummary"][]>([]);
    const [searches, setSearches] = useState<Schemas["CompanionSearchResponse"][]>([]);
    const [travelStyle, setTravelStyle] = useState<Schemas["TravelStyleResponse"]["items"]>([]);
    const [candidates, setCandidates] = useState<CandidateResult | null>(null);
    const [busy, setBusy] = useState(false);
    const [failed, setFailed] = useState(false);
    const [errorKey, setErrorKey] = useState<MatchingErrorKey>("matchingFlow.saveError");
    const failureReason = useRef<MatchingErrorKey>("matchingFlow.saveError");
    const [catalogFailed, setCatalogFailed] = useState(false);
    const [catalogLoading, setCatalogLoading] = useState(userId !== undefined);
    const [revision, setRevision] = useState(0);
    const pending = useRef<AbortController | null>(null);
    const locked = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        if (userId !== undefined) {
            void Promise.all([
                api.call("listDestinations", { signal: controller.signal }),
                api.call("listMyCompanionSearches", { signal: controller.signal }),
                api.call("getMyTravelStyle", { signal: controller.signal }),
            ])
                .then(([catalog, mine, style]) => {
                    if (controller.signal.aborted) return;
                    setDestinations(catalog.destinations);
                    setSearches(mine.searches);
                    setTravelStyle(style.items);
                })
                .catch(() => {
                    if (!controller.signal.aborted) setCatalogFailed(true);
                })
                .finally(() => {
                    if (!controller.signal.aborted) setCatalogLoading(false);
                });
        }
        return () => {
            controller.abort();
            pending.current?.abort();
            locked.current = false;
        };
    }, [userId, revision]);

    async function run(action: (signal: AbortSignal) => Promise<void>): Promise<boolean> {
        if (locked.current) return false;
        locked.current = true;
        pending.current?.abort();
        const controller = new AbortController();
        pending.current = controller;
        setBusy(true);
        setFailed(false);
        try {
            await action(controller.signal);
            return !controller.signal.aborted;
        } catch (error) {
            if (!controller.signal.aborted) {
                setFailed(true);
                failureReason.current =
                    error instanceof ApiError
                        ? (errorKeys[error.problem?.code ?? ""] ?? "matchingFlow.saveError")
                        : "matchingFlow.saveError";
                setErrorKey(failureReason.current);
                if (
                    error instanceof ApiError &&
                    error.problem?.code === "COMPANION_SEARCH_LIMIT_EXCEEDED"
                ) {
                    try {
                        const mine = await api.call("listMyCompanionSearches", {
                            signal: controller.signal,
                        });
                        if (!controller.signal.aborted) setSearches(mine.searches);
                    } catch {
                        /* Preserve the existing list when reconciliation is unavailable. */
                    }
                }
            }
            return false;
        } finally {
            if (pending.current === controller) {
                locked.current = false;
                setBusy(false);
            }
        }
    }

    const activeCount = searches.filter((search) => search.status === "ACTIVE").length;
    const limitReached = activeCount >= ACTIVE_RECRUITMENT_LIMIT;
    const canCreate = !catalogLoading && !catalogFailed && userId !== undefined && !limitReached;
    function denyCreate(): Promise<boolean> {
        setFailed(true);
        failureReason.current = limitReached
            ? "matchingFlow.activeLimit"
            : "matchingFlow.catalogError";
        setErrorKey(failureReason.current);
        return Promise.resolve(false);
    }
    function changeStatus(
        searchId: number,
        status: Schemas["CompanionSearchStatus"],
    ): Promise<boolean> {
        if (status === "ACTIVE" && !canCreate) return denyCreate();
        return run(async (signal) => {
            const response = await api.call("closeCompanionSearch", {
                params: { path: { searchId } },
                body: { status },
                signal,
            });
            if (!signal.aborted)
                setSearches((values) =>
                    values.map((value) => (value.id === searchId ? response : value)),
                );
        });
    }
    return {
        getFailureReason: () => failureReason.current,
        activeCount,
        limitReached,
        canCreate,
        errorKey,
        travelStyle,
        candidates,
        clearCandidates: () => {
            setCandidates(null);
            setFailed(false);
        },
        preview: (query: CandidateQuery) =>
            run(async (signal) => {
                const response = await api.call("previewCompanionCandidates", {
                    params: { query },
                    signal,
                });
                if (!signal.aborted) setCandidates(response);
            }),
        destinations,
        searches,
        busy,
        failed,
        catalogFailed,
        catalogLoading,
        reload: () => {
            setFailed(false);
            setCatalogFailed(false);
            setCatalogLoading(userId !== undefined);
            setRevision((value) => value + 1);
        },
        save: (body: MatchingConditions, searchId?: number) =>
            searchId === undefined && !canCreate
                ? denyCreate()
                : run(async (signal) => {
                      const response =
                          searchId === undefined
                              ? await api.call("createCompanionSearch", { body, signal })
                              : await api.call("updateCompanionSearch", {
                                    params: { path: { searchId } },
                                    body,
                                    signal,
                                });
                      if (!signal.aborted)
                          setSearches((values) => [
                              response,
                              ...values.filter((value) => value.id !== response.id),
                          ]);
                  }),
        close: (searchId: number) => changeStatus(searchId, "CLOSED"),
        resume: (searchId: number) => changeStatus(searchId, "ACTIVE"),
    };
}
