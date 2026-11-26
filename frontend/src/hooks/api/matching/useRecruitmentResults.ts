import { useEffect, useEffectEvent, useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";
import type {
    CandidateResult,
    CompanionRequest,
    MatchingRegistration,
    RequestDecision,
} from "@/hooks/matching.types";

import { pollWhileVisible } from "./pollWhileVisible";
import { useRequestMutation } from "./useCompanionRequests";

const SEARCH_DELAY_MS = 8_000;

async function readCandidates(searchId: number, signal: AbortSignal) {
    const candidates = new Map<number, CandidateResult["candidates"][number]>();
    for (let page = 0; ; page++) {
        const result = await api.call("listCompanionCandidates", {
            params: { path: { searchId }, query: { page, size: 100 } },
            signal,
        });
        signal.throwIfAborted();
        for (const candidate of result.candidates) candidates.set(candidate.searchId, candidate);
        if (!result.hasNext || result.candidates.length === 0) break;
    }
    return candidates;
}

type ResultState =
    | { phase: "searching" | "delayed" | "unavailable" | "offline" | "error" }
    | { phase: "loaded"; candidates: CandidateResult["candidates"]; archived: CompanionRequest[] };

export function useRecruitmentResults(
    searchId: number,
    revision: number | undefined,
    waitForJob: boolean,
    reloadPost: () => void,
    includeCandidates = true,
) {
    const [state, setState] = useState<ResultState>({ phase: "searching" });
    const [attempt, setAttempt] = useState(0);
    const mutation = useRequestMutation<number>();
    const responseMutation = useRequestMutation<number>();
    const generation = useRef(0);
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
                const readGeneration = generation.current;
                try {
                    setState((current) =>
                        current.phase === "offline" ? { phase: "searching" } : current,
                    );
                    if (includeCandidates) {
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
                                status.status === "RETRY" ||
                                Date.now() - started >= SEARCH_DELAY_MS;
                            setState({ phase: delayed ? "delayed" : "searching" });
                            return delayed ? 10000 : 2000;
                        }
                    }
                    const [candidates, history] = await Promise.all([
                        includeCandidates
                            ? readCandidates(searchId, signal)
                            : Promise.resolve(
                                  new Map<number, CandidateResult["candidates"][number]>(),
                              ),
                        api.call("listCompanionRequests", {
                            params: { path: { searchId } },
                            signal,
                        }),
                    ]);
                    if (signal.aborted) return null;
                    if (readGeneration !== generation.current) return null;
                    const archived = history.filter(
                        (request) => !candidates.has(request.candidateSearchId),
                    );
                    for (const request of history) {
                        const candidate = candidates.get(request.candidateSearchId);
                        if (candidate)
                            candidates.set(candidate.searchId, {
                                ...candidate,
                                requestStatus: request.status,
                                direction: request.direction,
                                requestId: request.requestId,
                                canRequest: false,
                            });
                    }
                    setState({ phase: "loaded", candidates: [...candidates.values()], archived });
                    return null;
                } catch (error) {
                    if (!signal.aborted && readGeneration === generation.current)
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
    }, [searchId, revision, waitForJob, attempt, includeCandidates]);
    return {
        state,
        busyId: mutation.busyKey,
        responseBusyId: responseMutation.busyKey,
        responseUncertain: responseMutation.uncertain,
        async respond(requestId: number, status: RequestDecision) {
            const result = await responseMutation.run(requestId, (signal) =>
                api.call("respondToCompanionRequest", {
                    params: { path: { requestId } },
                    body: { status },
                    signal,
                }),
            );
            if (!result) return false;
            generation.current++;
            setState((current) =>
                current.phase !== "loaded"
                    ? current
                    : {
                          ...current,
                          candidates: current.candidates.map((candidate) =>
                              candidate.requestId === requestId
                                  ? {
                                        ...candidate,
                                        requestStatus: result.status,
                                        direction: result.direction,
                                        canRequest: false,
                                    }
                                  : candidate,
                          ),
                          archived: current.archived.map((request) =>
                              request.requestId === requestId ? result : request,
                          ),
                      },
            );
            setAttempt((value) => value + 1);
            return result.status === status;
        },
        uncertain: mutation.uncertain,
        async send(candidateSearchId: number) {
            const result = await mutation.run(candidateSearchId, (signal) =>
                api.call("sendCompanionRequest", {
                    params: { path: { searchId } },
                    body: { candidateSearchId },
                    signal,
                }),
            );
            if (!result) return false;
            generation.current++;
            setState((current) =>
                current.phase !== "loaded"
                    ? current
                    : {
                          ...current,
                          candidates: current.candidates.map((candidate) =>
                              candidate.searchId === result.candidateSearchId
                                  ? {
                                        ...candidate,
                                        canRequest: false,
                                        direction: result.direction,
                                        requestId: result.requestId,
                                        requestStatus: result.status,
                                    }
                                  : candidate,
                          ),
                      },
            );
            setAttempt((n) => n + 1);
            return result.status === "PENDING" && result.direction === "SENT";
        },
        retry: () => {
            mutation.clearUncertain();
            responseMutation.clearUncertain();
            generation.current++;
            setState({ phase: "searching" });
            setAttempt((value) => value + 1);
        },
    };
}

export function useMatchedRecruitments(posts: MatchingRegistration[], enabled: boolean) {
    const [matchedRevisions, setMatchedRevisions] = useState<ReadonlyMap<number, number | null>>(
        new Map(),
    );
    const [requestSearchIds, setRequestSearchIds] = useState<ReadonlySet<number>>(new Set());
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
            const [answers, requestAnswers] = await Promise.all([
                Promise.allSettled(
                    active.map((post) =>
                        api.call("listCompanionCandidates", {
                            params: { path: { searchId: post.id }, query: { size: 1 } },
                            signal,
                        }),
                    ),
                ),
                Promise.allSettled(
                    posts.map((post) =>
                        api.call("listCompanionRequests", {
                            params: { path: { searchId: post.id } },
                            signal,
                        }),
                    ),
                ),
            ]);
            if (signal.aborted) return null;
            setFailed(
                [...answers, ...requestAnswers].some((answer) => answer.status === "rejected"),
            );
            setRequestSearchIds(
                (previous) =>
                    new Set(
                        posts
                            .filter((post, index) => {
                                const result = requestAnswers[index];
                                return result?.status === "fulfilled"
                                    ? result.value.length > 0
                                    : previous.has(post.id);
                            })
                            .map((post) => post.id),
                    ),
            );
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
            return posts.length ? 30000 : null;
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
    return { matchedIds, resultIds: new Set([...matchedIds, ...requestSearchIds]), failed };
}
