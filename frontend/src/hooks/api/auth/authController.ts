import type { ProfileDetailsUpdate } from "../../account.types.ts";
import type { components } from "../../../api/generated/schema.ts";
import { ApiError } from "../../../api/apiError.ts";
import {
    restoreSession,
    signIn,
    signOut,
    exchangeSocialSession,
    cancelSocialAuthentication,
    type AuthenticationResult,
} from "../../../api/auth/authSession.ts";
import {
    getAccessTokenSnapshot,
    getAuthRevision,
    subscribeAuthInvalidation,
    clearAccessToken,
} from "../../../api/auth/tokenStore.ts";
import type { AuthMessage, AuthState, LoginInput } from "../../auth.types.ts";
import { api } from "../../../api/api.ts";
import { isValidNickname } from "../../authValidation.ts";
import { invalidateOtherTabs } from "../../../api/auth/cookieCoordination.ts";

function needsLogin(error: unknown): boolean {
    return (
        error instanceof ApiError &&
        (error.kind === "authentication-required" ||
            error.kind === "aborted" ||
            error.status === 401)
    );
}

function messageFor(error: unknown, login: boolean): AuthMessage {
    if (!(error instanceof ApiError)) return "unavailable";
    if (login && error.problem?.code === "LOGIN_RATE_LIMITED") return "limited";
    if (login && error.problem?.code === "INVALID_CREDENTIALS") return "credentials";
    if (needsLogin(error)) return "expired";
    if (error.kind === "authentication-unavailable") return "browserUnavailable";
    if (error.problem?.code === "INVALID_CSRF_TOKEN") return "security";
    if (error.status === 400) return "invalidInput";
    if (login && (error.kind === "network" || error.kind === "invalid-response"))
        return "loginUncertain";
    return "unavailable";
}

export function socialMessageFor(error: unknown): AuthMessage {
    if (!(error instanceof ApiError)) return "socialUncertain";
    const code = error.problem?.code;
    if (code === "SOCIAL_AUTH_CANCELLED") return "socialCancelled";
    if (code === "SOCIAL_EMAIL_REQUIRED") return "socialEmailRequired";
    if (code === "SOCIAL_PROVIDER_DISABLED") return "socialDisabled";
    if (code === "EMAIL_ALREADY_REGISTERED" || code === "SOCIAL_ACCOUNT_CONFLICT")
        return "socialConflict";
    if (code === "INVALID_CSRF_TOKEN") return "security";
    if (error.kind === "authentication-unavailable") return "browserUnavailable";
    if (
        error.status === 400 ||
        error.status === 401 ||
        error.status === 409 ||
        error.status === 410
    )
        return "socialExpired";
    return "socialUncertain";
}

// 앱 인스턴스가 소유하는 회원 상태. 토큰·쿠키·갱신은 기존 인증 기반에서 관리한다.
export type AuthController = {
    getSnapshot: () => AuthState;
    subscribe: (listener: () => void) => () => void;
    connect: () => () => void;
    restore: () => Promise<boolean>;
    check: () => Promise<boolean>;
    login: (input: LoginInput) => Promise<boolean>;
    logout: () => Promise<boolean>;
    exchangeSocial: (code: string) => Promise<"signup" | "login" | false>;
    cancelSocial: () => void;
    resetDialog: () => void;
    updateProfileDetails: (input: ProfileDetailsUpdate, signal: AbortSignal) => Promise<void>;
    updateProfileImage: (profileImageUrl: string, signal: AbortSignal) => Promise<void>;
    uploadProfileImage: (dataUrl: string, signal: AbortSignal) => Promise<void>;
    updateNickname: (nickname: string, signal: AbortSignal) => Promise<void>;
    endSession: () => void;
};

export function createAuthController(): AuthController {
    let state: AuthState = { status: "checking", user: null, message: null };
    const listeners = new Set<() => void>();
    let generation = 0;
    let socialCompletion: "signup" | "login" = "login";
    let pending: { controller: AbortController; promise: Promise<boolean> } | undefined;

    function update(next: AuthState): void {
        state = next;
        listeners.forEach((listener) => listener());
    }

    function run(
        kind: "restore" | "login" | "logout",
        input?: LoginInput,
        social?: (signal: AbortSignal) => Promise<AuthenticationResult>,
    ): Promise<boolean> {
        // 클릭이나 StrictMode의 효과 재실행으로 인증 요청을 겹치지 않는다.
        if (pending) return Promise.resolve(false);
        const hadMember = state.user !== null;
        const current = ++generation;
        const controller = new AbortController();
        update({
            status: kind === "login" ? "signingIn" : kind === "logout" ? "signingOut" : "checking",
            user: null,
            message: null,
        });
        const promise = (async () => {
            try {
                const outcome = social ? await social(controller.signal) : undefined;
                if (outcome && outcome.revision !== getAuthRevision())
                    throw new ApiError("aborted");
                const user = outcome
                    ? outcome.user
                    : kind === "logout"
                      ? (await signOut(), null)
                      : kind === "login" && input
                        ? await signIn(input, { signal: controller.signal })
                        : await restoreSession({ signal: controller.signal });
                if (current !== generation) return false;
                if (user && !getAccessTokenSnapshot())
                    throw new ApiError("authentication-required");
                socialCompletion = outcome?.signedUp ? "signup" : "login";
                update({
                    status: user ? "authenticated" : "anonymous",
                    user,
                    message: null,
                });
                return true;
            } catch (error) {
                if (current !== generation) return false;
                const profilePending =
                    kind !== "logout" && !!getAccessTokenSnapshot() && !needsLogin(error);
                const anonymous = needsLogin(error) || kind === "login";
                const waitSeconds =
                    kind === "login" &&
                    error instanceof ApiError &&
                    error.problem?.code === "LOGIN_RATE_LIMITED"
                        ? error.problem.retryAfterSeconds
                        : undefined;
                const retryAt =
                    waitSeconds !== undefined &&
                    Number.isSafeInteger(waitSeconds) &&
                    waitSeconds > 0
                        ? Date.now() + waitSeconds * 1000
                        : undefined;
                update({
                    status: profilePending || !anonymous ? "unavailable" : "anonymous",
                    user: null,
                    ...(kind !== "logout" &&
                    error instanceof ApiError &&
                    error.status !== undefined &&
                    error.status >= 500
                        ? { failureStatus: error.status }
                        : {}),
                    ...(retryAt === undefined ? {} : { retryAt }),
                    ...(kind === "login" &&
                    error instanceof ApiError &&
                    error.problem?.remainingAttempts !== undefined
                        ? { remainingAttempts: error.problem.remainingAttempts }
                        : {}),
                    message:
                        kind === "logout"
                            ? "logoutUncertain"
                            : profilePending
                              ? "profileUnavailable"
                              : kind === "restore" && needsLogin(error)
                                ? hadMember
                                    ? "expired"
                                    : null
                                : social
                                  ? socialMessageFor(error)
                                  : messageFor(error, kind === "login"),
                });
                return false;
            } finally {
                if (current === generation) pending = undefined;
            }
        })();
        pending = { controller, promise };
        return promise;
    }

    async function updateProfile(
        body:
            | components["schemas"]["UpdateUserProfileRequest"]
            | components["schemas"]["UploadProfileImageRequest"],
        signal: AbortSignal,
    ): Promise<void> {
        if (!state.user || state.status !== "authenticated")
            throw new ApiError("authentication-required");
        const revision = getAuthRevision();
        const current = generation;
        const userId = state.user.userId;
        const user =
            "dataUrl" in body
                ? await api.call("uploadProfileImage", { body, signal })
                : await api.call("updateProfile", { body, signal });
        if (
            signal.aborted ||
            revision !== getAuthRevision() ||
            current !== generation ||
            state.user?.userId !== userId
        )
            throw new ApiError("aborted");
        update({ ...state, user });
    }

    return {
        getSnapshot: (): AuthState => state,
        updateNickname: async (nickname: string, signal: AbortSignal): Promise<void> => {
            if (!state.user || state.status !== "authenticated")
                throw new ApiError("authentication-required");
            if (!isValidNickname(nickname)) throw new ApiError("http", { status: 400 });
            await updateProfile({ nickname: nickname.trim() }, signal);
        },
        updateProfileDetails: (input: ProfileDetailsUpdate, signal: AbortSignal): Promise<void> =>
            updateProfile(input, signal),
        updateProfileImage: (profileImageUrl: string, signal: AbortSignal): Promise<void> =>
            updateProfile({ profileImageUrl }, signal),
        uploadProfileImage: (dataUrl: string, signal: AbortSignal): Promise<void> =>
            updateProfile({ dataUrl }, signal),
        endSession: (): void => {
            generation++;
            pending?.controller.abort();
            pending = undefined;
            clearAccessToken();
            invalidateOtherTabs();
            update({ status: "anonymous", user: null, message: null });
        },
        subscribe(listener: () => void): () => void {
            listeners.add(listener);
            return () => {
                listeners.delete(listener);
            };
        },
        connect(): () => void {
            const unsubscribe = subscribeAuthInvalidation((cause) => {
                if (state.status !== "authenticated") return;
                update({
                    status: needsLogin(cause) || cause === undefined ? "anonymous" : "unavailable",
                    user: null,
                    message: cause === undefined ? "expired" : messageFor(cause, false),
                });
            });
            return () => {
                unsubscribe();
                generation++;
                pending?.controller.abort();
                pending = undefined;
            };
        },
        restore: (): Promise<boolean> => pending?.promise ?? run("restore"),
        check: (): Promise<boolean> => {
            if (
                state.status !== "authenticated" ||
                (getAccessTokenSnapshot()?.expiresAt ?? 0) > Date.now()
            )
                return Promise.resolve(false);
            return run("restore");
        },
        login: (input: LoginInput): Promise<boolean> =>
            (state.retryAt ?? 0) > Date.now() ? Promise.resolve(false) : run("login", input),
        logout: (): Promise<boolean> => run("logout"),
        exchangeSocial: (code: string): Promise<"signup" | "login" | false> =>
            run("login", undefined, async () => {
                if (!/^[A-Za-z0-9_-]{43}$/.test(code))
                    throw new ApiError("http", {
                        problem: {
                            status: 400,
                            code: "SOCIAL_AUTH_INVALID",
                            title: "Invalid social result",
                            detail: "Start again",
                            instance: "/",
                        },
                    });
                return exchangeSocialSession(code);
            }).then((success) => (success ? socialCompletion : false)),
        cancelSocial: (): void => {
            if (pending) return;
            update({ status: "anonymous", user: null, message: null });
        },
        resetDialog: (): void => {
            if (pending && state.status === "signingIn") {
                generation++;
                pending.controller.abort();
                pending = undefined;
                cancelSocialAuthentication();
                clearAccessToken();
            }
            // 회원 조회·로그아웃 재시도 안내는 모달 밖에서도 유지한다.
            if (state.status === "unavailable") return;
            update({
                status: pending ? state.status : state.user ? "authenticated" : "anonymous",
                user: state.user,
                message: null,
            });
        },
    };
}
