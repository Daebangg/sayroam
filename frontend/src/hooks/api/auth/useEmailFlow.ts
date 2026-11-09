import { useEffect, useRef, useState } from "react";
import { api } from "../../../api/api.ts";
import { ApiError } from "../../../api/apiError.ts";
import { resetPassword } from "../../../api/auth/authSession.ts";
import { isValidEmail, isValidNewPassword, isValidNickname } from "../../authValidation.ts";
import type { EmailFlowMessage } from "../../auth.types.ts";

export type EmailFlowMode = "signup" | "reset";
type Verification = {
    email: string;
    verificationId: string;
    expiresAt: number;
    proof?: string;
    proofExpiresAt?: number;
};
type FlowMessage = EmailFlowMessage;

function flowMessage(error: unknown, completing: boolean): FlowMessage {
    if (!(error instanceof ApiError)) return "unavailable";
    const code = error.problem?.code;
    if (code === "INVALID_CSRF_TOKEN") return "security";
    if (code === "EMAIL_ALREADY_REGISTERED") return "registered";
    if (code === "EMAIL_CODE_INVALID") return "codeInvalid";
    if (error.status === 410) return "expired";
    if (error.status === 429) return "limited";
    if (code === "EMAIL_DELIVERY_FAILED" || code === "EMAIL_DELIVERY_DISABLED") return "delivery";
    if (
        completing &&
        (error.kind === "network" ||
            error.kind === "invalid-response" ||
            (error.status ?? 0) >= 500)
    )
        return "uncertain";
    if (error.status === 409 || code?.includes("PROOF_INVALID")) return "conflict";
    return "unavailable";
}

export type EmailFlow = {
    busy: boolean;
    message: EmailFlowMessage | null;
    sent: boolean;
    verified: boolean;
    expiresIn: number;
    resendIn: number;
    requestCode: (email: string) => Promise<boolean>;
    verifyCode: (code: string) => Promise<boolean>;
    complete: (input: { nickname: string; password: string }) => Promise<boolean>;
    invalidate: () => void;
};

export function useEmailFlow(mode: EmailFlowMode): EmailFlow {
    const [verification, setVerification] = useState<Verification | null>(null);
    const current = useRef<Verification | null>(null);
    const [resendAt, setResendAt] = useState(0);
    const [now, setNow] = useState(Date.now);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState<FlowMessage | null>(null);
    const pending = useRef<AbortController | null>(null);

    function remember(value: Verification | null): void {
        current.current = value;
        setVerification(value);
    }

    useEffect(
        () => () => {
            pending.current?.abort();
            pending.current = null;
        },
        [],
    );
    useEffect(() => {
        if (!verification && !resendAt) return;
        const timer = window.setInterval(() => setNow(Date.now()), 1000);
        return () => window.clearInterval(timer);
    }, [verification, resendAt]);

    async function execute(
        action: (signal: AbortSignal) => Promise<void>,
        completing = false,
    ): Promise<boolean> {
        if (pending.current) return false;
        const controller = new AbortController();
        pending.current = controller;
        setBusy(true);
        setMessage(null);
        try {
            await action(controller.signal);
            return !controller.signal.aborted;
        } catch (error) {
            if (controller.signal.aborted) return false;
            setMessage(flowMessage(error, completing));
            if (error instanceof ApiError && error.status === 429)
                setResendAt(Date.now() + (error.problem?.retryAfterSeconds ?? 60) * 1000);
            return false;
        } finally {
            if (pending.current === controller) {
                pending.current = null;
                setBusy(false);
                setNow(Date.now());
            }
        }
    }

    function invalidate(): void {
        pending.current?.abort();
        pending.current = null;
        setBusy(false);
        setMessage(null);
        remember(null);
        setResendAt(0);
    }

    function requestCode(email: string): Promise<boolean> {
        if (pending.current) return Promise.resolve(false);
        if (!isValidEmail(email)) {
            setMessage("invalidEmail");
            return Promise.resolve(false);
        }
        if (Date.now() < resendAt) return Promise.resolve(false);
        // 재발송 결과가 불확실하면 이전 코드·증명을 재사용하지 않는다.
        remember(null);
        return execute(async (signal) => {
            const issued =
                mode === "signup"
                    ? await api.call("request", { body: { email }, signal })
                    : await api.call("requestPasswordReset", { body: { email }, signal });
            if (signal.aborted) return;
            remember({
                email,
                verificationId: issued.verificationId,
                expiresAt: Date.now() + issued.expiresInSeconds * 1000,
            });
            setResendAt(Date.now() + issued.resendAfterSeconds * 1000);
        });
    }

    function verifyCode(code: string): Promise<boolean> {
        const saved = current.current;
        if (!saved || Date.now() >= saved.expiresAt) {
            setMessage("expired");
            return Promise.resolve(false);
        }
        if (!/^\d{6}$/.test(code)) {
            setMessage("codeInvalid");
            return Promise.resolve(false);
        }
        return execute(async (signal) => {
            const body = { email: saved.email, verificationId: saved.verificationId, code };
            const response =
                mode === "signup"
                    ? await api.call("confirm", { body, signal })
                    : await api.call("confirmPasswordReset", { body, signal });
            if (signal.aborted) return;
            remember({
                ...saved,
                proof: "signupProof" in response ? response.signupProof : response.resetProof,
                proofExpiresAt: Date.now() + response.expiresInSeconds * 1000,
            });
        });
    }

    function complete(input: { nickname: string; password: string }): Promise<boolean> {
        const saved = current.current;
        if (!saved?.proof || Date.now() >= (saved.proofExpiresAt ?? 0)) {
            setMessage("expired");
            return Promise.resolve(false);
        }
        if (mode === "signup" && !isValidNickname(input.nickname)) {
            setMessage("invalidNickname");
            return Promise.resolve(false);
        }
        if (!isValidNewPassword(input.password)) {
            setMessage("invalidPassword");
            return Promise.resolve(false);
        }
        if (pending.current) return Promise.resolve(false);
        const proof = saved.proof;
        remember(null);
        return execute(async (signal) => {
            if (mode === "signup")
                await api.call("signup", {
                    body: {
                        email: saved.email,
                        nickname: input.nickname,
                        password: input.password,
                        signupProof: proof,
                    },
                    signal,
                });
            else
                await resetPassword(
                    { email: saved.email, newPassword: input.password, resetProof: proof },
                    { signal },
                );
        }, true);
    }

    return {
        busy,
        message,
        sent: !!verification,
        verified: !!verification?.proof && now < (verification.proofExpiresAt ?? 0),
        expiresIn: Math.max(
            0,
            Math.ceil(
                ((verification?.proofExpiresAt ?? verification?.expiresAt ?? 0) - now) / 1000,
            ),
        ),
        resendIn: Math.max(0, Math.ceil((resendAt - now) / 1000)),
        requestCode,
        verifyCode,
        complete,
        invalidate,
    };
}
