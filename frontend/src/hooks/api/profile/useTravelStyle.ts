import { useCallback, useEffect, useRef, useState } from "react";
import { api } from "@/api/api";

import {
    profileStyleQuestions,
    profileStyleCodes,
    hasCompleteStyle,
    type ProfileStyleValues,
} from "@/data/profileStyle";

export function useTravelStyle(userId: number | undefined) {
    const [values, setValues] = useState<ProfileStyleValues | null>(null);
    const [failed, setFailed] = useState(false);
    const [busy, setBusy] = useState(false);
    const [saved, setSaved] = useState(false);
    const pending = useRef<AbortController | null>(null);
    const loadingRequest = useRef<AbortController | null>(null);
    const reload = useCallback(() => {
        if (userId === undefined) return;
        loadingRequest.current?.abort();
        const controller = new AbortController();
        loadingRequest.current = controller;
        void api
            .call("getMyTravelStyle", { signal: controller.signal })
            .then((response) => {
                if (controller.signal.aborted) return;
                setFailed(false);
                setValues(
                    Object.fromEntries(
                        profileStyleQuestions.map((key) => [
                            key,
                            response.items.find((item) => item.code === profileStyleCodes[key])
                                ?.score ?? 0,
                        ]),
                    ),
                );
            })
            .catch(() => {
                if (!controller.signal.aborted) setFailed(true);
            });
    }, [userId]);
    useEffect(() => {
        reload();
        return () => {
            loadingRequest.current?.abort();
            pending.current?.abort();
        };
    }, [reload, userId]);
    async function save(next: ProfileStyleValues): Promise<boolean> {
        if (pending.current || !hasCompleteStyle(next)) return false;
        const controller = new AbortController();
        pending.current = controller;
        loadingRequest.current?.abort();
        setBusy(true);
        setFailed(false);
        setSaved(false);
        try {
            await api.call("updateMyTravelStyle", {
                body: {
                    answers: profileStyleQuestions.map((key) => ({
                        code: profileStyleCodes[key],
                        score: next[key]!,
                    })),
                },
                signal: controller.signal,
            });
            if (controller.signal.aborted) return false;
            setValues(next);
            setSaved(true);
            return true;
        } catch {
            if (!controller.signal.aborted) setFailed(true);
            return false;
        } finally {
            if (pending.current === controller) {
                pending.current = null;
                if (!controller.signal.aborted) setBusy(false);
            }
        }
    }
    return { values, failed, busy, saved, reload, save, dismissSaved: () => setSaved(false) };
}
