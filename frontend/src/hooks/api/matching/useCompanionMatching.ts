import { useEffect, useRef, useState } from "react";
import type { CandidateQuery, CandidateResult } from "@/hooks/matching.types";
import { api } from "@/api/api";
import type { components } from "@/api/generated/schema";

type Schemas = components["schemas"];
export type MatchingConditions = Schemas["SaveCompanionSearchRequest"];

export function useCompanionMatching(userId: number | undefined) {
    const [destinations, setDestinations] = useState<Schemas["DestinationSummary"][]>([]);
    const [searches, setSearches] = useState<Schemas["CompanionSearchResponse"][]>([]);
    const [candidates, setCandidates] = useState<CandidateResult | null>(null);
    const [busy, setBusy] = useState(false);
    const [failed, setFailed] = useState(false);
    const [catalogFailed, setCatalogFailed] = useState(false);
    const [catalogLoading, setCatalogLoading] = useState(userId !== undefined);
    const [revision, setRevision] = useState(0);
    const pending = useRef<AbortController | null>(null);
    const locked = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        setCandidates(null);
        setDestinations([]);
        setSearches([]);
        setFailed(false);
        setCatalogFailed(false);
        setCatalogLoading(userId !== undefined);
        if (userId !== undefined) {
            void Promise.all([
                api.call("listDestinations", { signal: controller.signal }),
                api.call("listMyCompanionSearches", { signal: controller.signal }),
            ])
                .then(([catalog, mine]) => {
                    if (controller.signal.aborted) return;
                    setDestinations(catalog.destinations);
                    setSearches(mine.searches);
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
        } catch {
            if (!controller.signal.aborted) setFailed(true);
            return false;
        } finally {
            if (pending.current === controller) {
                locked.current = false;
                setBusy(false);
            }
        }
    }

    return {
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
        reload: () => setRevision((value) => value + 1),
        save: (body: MatchingConditions, searchId?: number) =>
            run(async (signal) => {
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
        close: (searchId: number) =>
            run(async (signal) => {
                const response = await api.call("closeCompanionSearch", {
                    params: { path: { searchId } },
                    body: { status: "CLOSED" },
                    signal,
                });
                if (!signal.aborted)
                    setSearches((values) =>
                        values.map((value) => (value.id === searchId ? response : value)),
                    );
            }),
    };
}
