import { useEffect, useRef, useState } from "react";
import { ApiError } from "../../../api/apiError.ts";
import { isValidNickname } from "../../authValidation.ts";
import type { NicknameEditor } from "../../account.types.ts";

export function useNicknameEditor(
    update: (nickname: string, signal: AbortSignal) => Promise<void>,
): NicknameEditor {
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState<"invalid" | "unavailable" | "uncertain" | null>(null);
    const pending = useRef<AbortController | null>(null);
    useEffect(
        () => () => {
            pending.current?.abort();
        },
        [],
    );
    async function save(nickname: string): Promise<boolean> {
        if (pending.current) return false;
        if (!isValidNickname(nickname)) {
            setMessage("invalid");
            return false;
        }
        const controller = new AbortController();
        pending.current = controller;
        setBusy(true);
        setMessage(null);
        try {
            await update(nickname, controller.signal);
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
            if (!controller.signal.aborted) setBusy(false);
            pending.current = null;
        }
    }
    return { busy, message, save, reset: () => setMessage(null) };
}
