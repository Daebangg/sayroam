import { api } from "../../../api/api.ts";
import { ApiError } from "../../../api/apiError.ts";
import { getAuthRevision } from "../../../api/auth/tokenStore.ts";
import { isValidNewPassword } from "../../authValidation.ts";
import type {
    AccountFlowView,
    AccountMessage,
    AccountPurpose,
    AccountResult,
} from "../../account.types.ts";

function messageFor(error: unknown): AccountMessage {
    if (!(error instanceof ApiError)) return "unavailable";
    const code = error.problem?.code;
    if (code === "PASSWORD_CHANGE_NOT_ALLOWED") return "passwordNotAllowed";
    if (code === "ACCOUNT_VERIFICATION_NOT_ALLOWED") return "verificationNotAllowed";
    if (code === "INVALID_CSRF_TOKEN") return "security";
    if (code === "EMAIL_CODE_INVALID") return "codeInvalid";
    if (code === "PASSWORD_POLICY_VIOLATION") return "invalidPassword";
    if (code === "EMAIL_DELIVERY_FAILED" || code === "EMAIL_DELIVERY_DISABLED") return "delivery";
    if (error.status === 410) return "expired";
    if (error.status === 429) return "limited";
    if (error.status === 409 || code === "ACCOUNT_PROOF_INVALID") return "conflict";
    if (error.kind === "network" || error.kind === "invalid-response") return "uncertain";
    return "unavailable";
}

export function accountResultForError(error: unknown): AccountResult["outcome"] | null {
    if (!(error instanceof ApiError)) return "uncertain";
    if (
        error.problem?.code === "PASSWORD_CHANGE_CONFLICT" ||
        error.problem?.code === "ACCOUNT_DELETION_CONFLICT"
    )
        return "reauthenticate";
    if (error.kind === "network" || error.kind === "invalid-response" || error.kind === "aborted")
        return "uncertain";
    if (
        (error.status ?? 0) >= 500 &&
        !["AUTHENTICATION_UNAVAILABLE", "EMAIL_VERIFICATION_UNAVAILABLE"].includes(
            error.problem?.code ?? "",
        )
    )
        return "uncertain";
    return null;
}

export function createAccountController(
    purpose: AccountPurpose,
    onResult: (result: AccountResult) => void,
) {
    let state: AccountFlowView = {
        stage: "request",
        busy: false,
        sending: false,
        message: null,
        expiresAt: 0,
        resendAt: 0,
    };
    let verification: { id: string; revision: number; proof?: string } | null = null;
    let pending: AbortController | null = null;
    let generation = 0;
    const listeners = new Set<() => void>();
    function update(patch: Partial<AccountFlowView>): void {
        state = { ...state, ...patch };
        listeners.forEach((listener) => listener());
    }
    async function execute(
        action: (signal: AbortSignal) => Promise<void>,
        completing = false,
        sending = false,
    ): Promise<boolean> {
        if (pending) return false;
        const controller = new AbortController();
        const current = generation;
        const revision = getAuthRevision();
        pending = controller;
        update({ busy: true, sending, message: null });
        try {
            await action(controller.signal);
            return !controller.signal.aborted;
        } catch (error) {
            if (completing && revision === getAuthRevision()) {
                const outcome = accountResultForError(error);
                if (outcome) {
                    onResult({ purpose, outcome });
                    return false;
                }
            }
            if (controller.signal.aborted || current !== generation) return false;
            const message = messageFor(error);
            const retry =
                error instanceof ApiError && error.status === 429
                    ? (error.problem?.retryAfterSeconds ?? 60)
                    : 0;
            const reset =
                completing ||
                ["expired", "conflict", "uncertain"].includes(message) ||
                (error instanceof ApiError &&
                    error.problem?.code === "EMAIL_CODE_ATTEMPTS_EXCEEDED");
            if (reset) verification = null;
            update({
                message,
                ...(reset ? { stage: "request", expiresAt: 0 } : {}),
                ...(retry ? { resendAt: Date.now() + retry * 1000 } : {}),
            });
            return false;
        } finally {
            if (pending === controller) {
                pending = null;
                update({ busy: false, sending: false });
            }
        }
    }
    return {
        getSnapshot: (): AccountFlowView => state,
        subscribe(listener: () => void): () => void {
            listeners.add(listener);
            return () => {
                listeners.delete(listener);
            };
        },
        dispose(): void {
            generation++;
            pending?.abort();
            pending = null;
            verification = null;
        },
        requestCode(): Promise<boolean> {
            if (pending || Date.now() < state.resendAt) return Promise.resolve(false);
            verification = null;
            update({ stage: "request", expiresAt: 0 });
            return execute(
                async (signal) => {
                    const revision = getAuthRevision();
                    const issued = await api.call("request_1", { body: { purpose }, signal });
                    if (signal.aborted) return;
                    if (issued.purpose !== purpose) throw new ApiError("invalid-response");
                    verification = { id: issued.verificationId, revision };
                    update({
                        stage: "code",
                        expiresAt: Date.now() + issued.expiresInSeconds * 1000,
                        resendAt: Date.now() + issued.resendAfterSeconds * 1000,
                    });
                },
                false,
                true,
            );
        },
        verifyCode(code: string): Promise<boolean> {
            const saved = verification;
            if (
                !saved ||
                saved.revision !== getAuthRevision() ||
                state.stage !== "code" ||
                Date.now() >= state.expiresAt
            ) {
                update({ message: "expired" });
                return Promise.resolve(false);
            }
            if (!/^\d{6}$/.test(code)) {
                update({ message: "codeInvalid" });
                return Promise.resolve(false);
            }
            return execute(async (signal) => {
                const confirmed = await api.call("confirm_1", {
                    body: { purpose, verificationId: saved.id, code },
                    signal,
                });
                if (signal.aborted) return;
                if (confirmed.purpose !== purpose) throw new ApiError("invalid-response");
                verification = { ...saved, proof: confirmed.accountProof };
                update({
                    stage: "verified",
                    expiresAt: Date.now() + confirmed.expiresInSeconds * 1000,
                });
            });
        },
        complete(password: string, deletionConfirmed: boolean): Promise<boolean> {
            const saved = verification;
            if (
                !saved?.proof ||
                saved.revision !== getAuthRevision() ||
                state.stage !== "verified" ||
                Date.now() >= state.expiresAt
            ) {
                update({ message: "expired" });
                return Promise.resolve(false);
            }
            if (pending) return Promise.resolve(false);
            if (purpose === "PASSWORD_CHANGE" && !isValidNewPassword(password)) {
                update({ message: "invalidPassword" });
                return Promise.resolve(false);
            }
            if (purpose === "ACCOUNT_DELETION" && !deletionConfirmed) return Promise.resolve(false);
            const accountProof = saved.proof;
            verification = null;
            return execute(async (signal) => {
                if (purpose === "PASSWORD_CHANGE")
                    await api.call("change", {
                        body: { accountProof, newPassword: password },
                        signal,
                    });
                else await api.call("delete", { body: { accountProof }, signal });
                onResult({ purpose, outcome: "complete" });
            }, true);
        },
    };
}
