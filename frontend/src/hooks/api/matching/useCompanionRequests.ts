import { useEffect, useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";
import type { CompanionRequest, RequestDecision } from "@/hooks/matching.types";
import { pollWhileVisible } from "./pollWhileVisible";

// 쓰기 결과가 불확실하면 GET으로 상태를 확인할 때까지 같은 작업을 다시 보내지 않는다.
export function useRequestMutation<Key extends number | RequestDecision>() {
    const active = useRef<AbortController | null>(null);
    const [busyKey, setBusyKey] = useState<Key>();
    const [uncertain, setUncertain] = useState<ReadonlySet<Key>>(new Set());
    useEffect(() => () => active.current?.abort(), []);
    async function run(key: Key, change: (signal: AbortSignal) => Promise<CompanionRequest>) {
        if (active.current || uncertain.has(key)) return;
        const controller = new AbortController();
        active.current = controller;
        setBusyKey(key);
        try {
            const request = await change(controller.signal);
            if (!controller.signal.aborted) return request;
        } catch {
            if (!controller.signal.aborted) setUncertain((keys) => new Set(keys).add(key));
        } finally {
            active.current = null;
            if (!controller.signal.aborted) setBusyKey(undefined);
        }
    }
    return { run, busyKey, uncertain, clearUncertain: () => setUncertain(new Set()) };
}

export function useCompanionRequest(id: number) {
    const [request, setRequest] = useState<CompanionRequest>();
    const [error, setError] = useState<"unavailable" | "loadError">();
    const [version, setVersion] = useState(0);
    const generation = useRef(0);
    const reload = () => setVersion((n) => n + 1);

    useEffect(() => {
        return pollWhileVisible(
            async (signal) => {
                const readGeneration = generation.current;
                try {
                    const result = await api.call("getCompanionRequest", {
                        params: { path: { requestId: id } },
                        signal,
                    });
                    if (!signal.aborted && generation.current === readGeneration) {
                        setRequest(result);
                        setError(undefined);
                    }
                } catch (cause) {
                    if (!signal.aborted && generation.current === readGeneration)
                        setError(
                            cause instanceof ApiError && [403, 404].includes(cause.status ?? 0)
                                ? "unavailable"
                                : "loadError",
                        );
                }
                return null;
            },
            () => setError("loadError"),
        );
    }, [id, version]);

    function recheck() {
        generation.current++;
        setRequest(undefined);
        setError(undefined);
        reload();
    }
    return {
        request,
        error,
        recheck,
    };
}
