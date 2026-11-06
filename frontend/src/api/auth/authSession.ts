import { ApiError } from "../apiError.ts";
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

export async function signIn(
    input: LoginRequest,
    options: AuthCallOptions = {},
): Promise<CurrentUserResponse> {
    // 같은 탭에서 로그인 요청이 겹쳐 Refresh 쿠키가 서로 덮어쓰이지 않게 한다.
    if (signingIn) throw new ApiError("authentication-in-progress");
    if (options.signal?.aborted) throw new ApiError("aborted", { cause: options.signal.reason });

    signingIn = true;
    clearAccessToken();
    invalidateOtherTabs();
    const attemptRevision = getAuthRevision();
    try {
        let response: LoginResponse;
        try {
            response = await loginApi(input, options);
        } catch (error) {
            assertCurrentAttempt(attemptRevision, options);
            throw error;
        }
        assertCurrentAttempt(attemptRevision, options);
        setAccessToken(response);

        const sessionRevision = getAuthRevision();
        const user = await getCurrentUser(options);
        assertCurrentAttempt(sessionRevision, options);
        return user;
    } finally {
        signingIn = false;
    }
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
