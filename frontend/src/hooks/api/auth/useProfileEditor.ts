import { useEffect, useRef, useState } from "react";
import { ApiError } from "../../../api/apiError.ts";

import type { ProfileEditor } from "../../account.types.ts";

export function useProfileEditor<T>(
    update: (input: T, signal: AbortSignal) => Promise<void>,
): ProfileEditor<T> {
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState<"invalid" | "uncertain" | "unavailable" | null>(null);
    const pending = useRef<AbortController | null>(null);
    useEffect(() => () => pending.current?.abort(), []);
    async function save(input: T): Promise<boolean> {
        if (pending.current) return false;
        const controller = new AbortController();
        pending.current = controller;
        setBusy(true);
        setMessage(null);
        try {
            await update(input, controller.signal);
            return !controller.signal.aborted;
        } catch (error) {
            if (!controller.signal.aborted)
                setMessage(
                    error instanceof ApiError && error.status === 400
                        ? "invalid"
                        : error instanceof ApiError &&
                            (error.kind === "network" ||
                                error.kind === "invalid-response" ||
                                (error.status ?? 0) >= 500)
                          ? "uncertain"
                          : "unavailable",
                );
            return false;
        } finally {
            pending.current = null;
            if (!controller.signal.aborted) setBusy(false);
        }
    }
    return { busy, message, save, reset: () => setMessage(null) };
}
