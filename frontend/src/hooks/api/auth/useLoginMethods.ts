import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "../../../api/api.ts";
import type { LoginMethods } from "../../account.types.ts";

export function useLoginMethods(userId: number) {
    const [methods, setMethods] = useState<LoginMethods | null>(null);
    const [failed, setFailed] = useState(false);
    const pending = useRef<AbortController | null>(null);
    const reload = useCallback((): void => {
        pending.current?.abort();
        const controller = new AbortController();
        pending.current = controller;
        void api
            .call("loginMethods", { signal: controller.signal })
            .then((value) => {
                if (!controller.signal.aborted) {
                    setMethods(value);
                    setFailed(false);
                }
            })
            .catch(() => {
                if (!controller.signal.aborted) setFailed(true);
            });
    }, []);
    useEffect(() => {
        reload();
        return () => pending.current?.abort();
    }, [reload, userId]);
    return { methods, failed, reload };
}
