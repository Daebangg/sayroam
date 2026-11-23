import { useEffect, useEffectEvent, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";
import type { CandidateResult, MatchingRegistration } from "@/hooks/matching.types";

const SEARCH_DELAY_MS = 8_000;

// 응답이 끝난 뒤에만 다음 조회를 예약한다. 화면을 벗어나면 읽기 요청도 취소한다.
function pollWhileVisible(
    read: (signal: AbortSignal) => Promise<number | null>,
    onOffline?: () => void,
) {
    let controller: AbortController | undefined;
    let timer: number | undefined;
    let disposed = false;
    async function refresh() {
        if (disposed || controller || document.hidden || !navigator.onLine) return;
        window.clearTimeout(timer);
        const request = new AbortController();
        controller = request;
        try {
            const delay = await read(request.signal);
            if (delay !== null && !request.signal.aborted)
                timer = window.setTimeout(() => void refresh(), delay);
        } finally {
            if (controller === request) controller = undefined;
        }
    }
    function pause() {
        window.clearTimeout(timer);
        controller?.abort();
        controller = undefined;
    }
    function update() {
        if (document.hidden || !navigator.onLine) {
            pause();
            if (!navigator.onLine) onOffline?.();
        } else void refresh();
    }
    document.addEventListener("visibilitychange", update);
    window.addEventListener("focus", update);
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    update();
    return () => {
        disposed = true;
        pause();
        document.removeEventListener("visibilitychange", update);
        window.removeEventListener("focus", update);
        window.removeEventListener("online", update);
        window.removeEventListener("offline", update);
    };
}

type ResultState =
    | { phase: "searching" | "delayed" | "unavailable" | "offline" | "error" }
    | { phase: "loaded"; candidates: CandidateResult["candidates"] };

export function useRecruitmentResults(
    searchId: number,
    revision: number | undefined,
    waitForJob: boolean,
    reloadPost: () => void,
) {
    const [state, setState] = useState<ResultState>({ phase: "searching" });
    const [attempt, setAttempt] = useState(0);
    const syncRevision = useEffectEvent(reloadPost);
    useEffect(() => {
        if (!waitForJob || state.phase !== "searching") return;
        const timer = window.setTimeout(() => {
            setState((current) => (current.phase === "searching" ? { phase: "delayed" } : current));
        }, SEARCH_DELAY_MS);
        return () => window.clearTimeout(timer);
    }, [waitForJob, state.phase, attempt]);
    useEffect(() => {
        const started = Date.now();
        return pollWhileVisible(
            async (signal) => {
                try {
                    setState((current) =>
                        current.phase === "offline" ? { phase: "searching" } : current,
                    );
                    const status = await api.call("getCompanionMatchingStatus", {
                        params: { path: { searchId } },
                        signal,
                    });
                    if (signal.aborted) return null;
                    if (revision !== undefined && status.matchingRevision !== revision) {
                        syncRevision();
                        return null;
                    }
                    if (status.status === "SKIPPED") {
                        setState({ phase: "unavailable" });
                        return null;
                    }
                    if (
                        waitForJob &&
                        (status.status === "PENDING" ||
                            status.status === "PROCESSING" ||
                            status.status === "RETRY")
                    ) {
                        const delayed =
                            status.status === "RETRY" || Date.now() - started >= SEARCH_DELAY_MS;
                        setState({ phase: delayed ? "delayed" : "searching" });
                        return delayed ? 10000 : 2000;
                    }
                    const candidates = new Map<number, CandidateResult["candidates"][number]>();
                    for (let page = 0; ; page++) {
                        const result = await api.call("listCompanionCandidates", {
                            params: { path: { searchId }, query: { page, size: 100 } },
                            signal,
                        });
                        if (signal.aborted) return null;
                        for (const candidate of result.candidates)
                            candidates.set(candidate.searchId, candidate);
                        if (!result.hasNext || result.candidates.length === 0) break;
                    }
                    setState({ phase: "loaded", candidates: [...candidates.values()] });
                    return null;
                } catch (error) {
                    if (!signal.aborted)
                        setState({
                            phase:
                                error instanceof ApiError && [404, 409].includes(error.status ?? 0)
                                    ? "unavailable"
                                    : "error",
                        });
                }
                return null;
            },
            () => setState({ phase: "offline" }),
        );
    }, [searchId, revision, waitForJob, attempt]);
    return {
        state,
        retry: () => {
            setState({ phase: "searching" });
            setAttempt((value) => value + 1);
        },
    };
}

export function useMatchedRecruitments(posts: MatchingRegistration[], enabled: boolean) {
    const [matchedRevisions, setMatchedRevisions] = useState<ReadonlyMap<number, number | null>>(
        new Map(),
    );
    const [failed, setFailed] = useState(false);
    useEffect(() => {
        if (!enabled) return;
        const active = posts.filter(
            (post) =>
                post.status === "ACTIVE" &&
                post.destinationId !== null &&
                post.matchingStatus !== "SKIPPED",
        );
        return pollWhileVisible(async (signal) => {
            const answers = await Promise.allSettled(
                active.map((post) =>
                    api.call("listCompanionCandidates", {
                        params: { path: { searchId: post.id }, query: { size: 1 } },
                        signal,
                    }),
                ),
            );
            if (signal.aborted) return null;
            setFailed(answers.some((answer) => answer.status === "rejected"));
            setMatchedRevisions((previous) => {
                const next = new Map<number, number | null>();
                active.forEach((post, index) => {
                    const answer = answers[index];
                    if (!answer) return;
                    const matched =
                        answer.status === "fulfilled"
                            ? answer.value.candidates.length > 0
                            : !(
                                  answer.reason instanceof ApiError &&
                                  [404, 409].includes(answer.reason.status ?? 0)
                              ) && previous.get(post.id) === (post.matchingRevision ?? null);
                    if (matched) next.set(post.id, post.matchingRevision ?? null);
                });
                return next;
            });
            return active.length ? 30000 : null;
        });
    }, [enabled, posts]);
    const matchedIds = new Set(
        posts
            .filter(
                (post) =>
                    post.status === "ACTIVE" &&
                    matchedRevisions.get(post.id) === (post.matchingRevision ?? null),
            )
            .map((post) => post.id),
    );
    return { matchedIds, failed };
}
