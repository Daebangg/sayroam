import type { components } from "../generated/schema.ts";
import { ApiError } from "../apiError.ts";
import { api } from "../api.ts";
import type { ApiCallOptions } from "../router.ts";
import { waitForSharedRequest } from "../abort.ts";
import { getCurrentUser, type CurrentUserResponse } from "../users/userApi.ts";
import {
    loginApi,
    logoutApi,
    refreshApi,
    type AuthCallOptions,
    type LoginRequest,
    type LoginResponse,
} from "./authApi.ts";
import { invalidateOtherTabs } from "./cookieCoordination.ts";
import { refreshAccessToken } from "./refreshSession.ts";
import {
    clearAccessToken,
    getAccessTokenSnapshot,
    getAuthRevision,
    setAccessToken,
} from "./tokenStore.ts";

let signingIn = false;

function assertCurrentAttempt(revision: number, options: AuthCallOptions): void {
    if (revision !== getAuthRevision() || options.signal?.aborted) {
        throw new ApiError("aborted", { cause: options.signal?.reason });
    }
}

export type AuthenticationResult = {
    revision: number;
    user: CurrentUserResponse;
};

async function authenticate(
    execute: () => Promise<LoginResponse | components["schemas"]["SocialExchangeResponse"]>,
    options: AuthCallOptions = {},
): Promise<AuthenticationResult> {
    // 같은 탭에서 로그인 요청이 겹쳐 Refresh 쿠키가 서로 덮어쓰이지 않게 한다.
    if (signingIn) throw new ApiError("authentication-in-progress");
    if (options.signal?.aborted) throw new ApiError("aborted", { cause: options.signal.reason });

    signingIn = true;
    clearAccessToken();
    invalidateOtherTabs();
    const attemptRevision = getAuthRevision();
    try {
        let response: LoginResponse | components["schemas"]["SocialExchangeResponse"];
        try {
            response = await execute();
        } catch (error) {
            assertCurrentAttempt(attemptRevision, options);
            throw error;
        }
        assertCurrentAttempt(attemptRevision, options);
        if ("status" in response && response.status === "SIGNUP_REQUIRED" && response.signup) {
            // 이전 서버에서 받은 티켓도 이름 입력·인증 메일 화면 없이 한 번만 완료한다.
            if (response.signup.emailVerificationRequired || !response.signup.suggestedEmail)
                throw new ApiError("http", {
                    status: 400,
                    problem: {
                        status: 400,
                        code: "SOCIAL_EMAIL_REQUIRED",
                        title: "Email consent required",
                        detail: "Allow access to your social account email",
                        instance: "/",
                    },
                });
            response = await api.call("completeSocialSignup", {
                ...options,
                body: { signupTicket: response.signup.signupTicket },
            });
            assertCurrentAttempt(attemptRevision, options);
        }
        const login = "status" in response ? response.login : response;
        if (!login) throw new ApiError("invalid-response");
        setAccessToken(login);

        const sessionRevision = getAuthRevision();
        const user = await getCurrentUser(options);
        assertCurrentAttempt(sessionRevision, options);
        return { user, revision: getAuthRevision() };
    } finally {
        signingIn = false;
    }
}

export async function signIn(
    input: LoginRequest,
    options: AuthCallOptions = {},
): Promise<CurrentUserResponse> {
    const result = await authenticate(() => loginApi(input, options), options);
    if (!result.user) throw new ApiError("invalid-response");
    return result.user;
}

const socialExchanges = new Map<string, Promise<AuthenticationResult>>();
const socialRequests = new Set<AbortController>();
export function cancelSocialAuthentication(): void {
    for (const controller of socialRequests) controller.abort();
}
export function exchangeSocialSession(resultCode: string): Promise<AuthenticationResult> {
    let pending = socialExchanges.get(resultCode);
    if (!pending) {
        const controller = new AbortController();
        socialRequests.add(controller);
        pending = authenticate(
            () =>
                api.call("exchangeSocialLoginResult", {
                    body: { resultCode },
                    signal: controller.signal,
                }),
            { signal: controller.signal },
        ).finally(() => socialRequests.delete(controller));
        // 결과 코드는 일회성이다. StrictMode·재마운트에서도 실패한 POST를 재실행하지 않는다.
        socialExchanges.set(resultCode, pending);
    }
    return pending;
}

export async function restoreSession(options: AuthCallOptions = {}): Promise<CurrentUserResponse> {
    if (options.signal?.aborted) throw new ApiError("aborted");
    const revision = getAuthRevision();
    if (!getAccessTokenSnapshot()) {
        await waitForSharedRequest(refreshAccessToken(refreshApi, revision), options.signal);
    }
    assertCurrentAttempt(revision, options);
    return getCurrentUser(options);
}

export async function signOut(): Promise<void> {
    // 서버 응답을 기다리지 않고 이전 요청과 메모리 인증을 먼저 무효화한다.
    clearAccessToken();
    invalidateOtherTabs();
    // 실패를 성공으로 숨기지 않는다. 쿠키 폐기 결과가 불확실하면 호출자가 안내한다.
    await logoutApi();
}

export async function resetPassword(
    input: ApiCallOptions<"resetPassword">["body"],
    options: AuthCallOptions = {},
): Promise<void> {
    clearAccessToken();
    invalidateOtherTabs();
    await api.call("resetPassword", { ...options, body: input });
}
