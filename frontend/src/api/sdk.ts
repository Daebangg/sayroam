import { ApiError } from "./apiError.ts";
import { waitForSharedRequest } from "./abort.ts";
import { getAuthRevision } from "./auth/tokenStore.ts";
import { refreshAccessToken } from "./auth/refreshSession.ts";
import { assertTokenUsage, validateToken } from "./auth/tokenResponse.ts";
import { refresh as generatedRefresh } from "./generated/sdk/sdk.gen.js";
import { invalidateOtherTabs, withAuthCookieLock } from "./auth/cookieCoordination.ts";
import { client } from "./generated/sdk/client.gen.js";
import { withRequestPolicy } from "./http.ts";
import { sendResponse, type RequestOptions } from "./transport.ts";

// 생성 파일 대신 이 진입점을 사용해야 공통 인증·CSRF·오류 정책이 적용된다.
export * from "./generated/sdk/sdk.gen.js";

const origin = typeof window === "undefined" ? "http://sayroam.invalid" : window.location.origin;
type RequestPolicy = {
    auth: "none" | "required";
    revision: number;
    noContent: boolean;
    validateEmpty: ((data: unknown) => Promise<unknown>) | undefined;
};
const policies = new WeakMap<Request, RequestPolicy>();
const contexts = new WeakMap<object, RequestPolicy>();

function isApiPath(path: string): path is `/api/${string}` {
    return path.startsWith("/api/");
}

client.setConfig({
    baseUrl: origin,
    credentials: "same-origin",
    parseAs: "json",
    throwOnError: true,
    fetch: async (input) => {
        if (!(input instanceof Request)) throw new ApiError("invalid-response");
        const url = new URL(input.url);
        const path = url.pathname + url.search;
        if (url.origin !== origin || !isApiPath(path)) throw new ApiError("invalid-response");
        const policy = policies.get(input);
        if (!policy) throw new ApiError("invalid-response");
        const options: RequestOptions = {
            method: input.method,
            headers: input.headers,
            signal: input.signal,
            cache: input.cache,
        };
        if (input.body !== null) options.body = await input.text();
        const cookieAction =
            url.pathname === "/api/auth/login"
                ? "login"
                : url.pathname === "/api/auth/refresh"
                  ? "refresh"
                  : url.pathname === "/api/auth/logout"
                    ? "logout"
                    : undefined;
        const execute = async (): Promise<Response> => {
            if (policy.revision !== getAuthRevision()) throw new ApiError("aborted");
            if (url.pathname === "/api/auth/login" || url.pathname === "/api/auth/logout") {
                invalidateOtherTabs();
            }
            const response = await withRequestPolicy(
                path,
                { ...options, auth: policy.auth },
                sendResponse,
                (revision) =>
                    refreshAccessToken(
                        async () => validateToken(await generatedRefresh()),
                        revision,
                    ),
            );
            policy.noContent =
                response.status === 204 || response.status === 205 || input.method === "HEAD";
            if (policy.noContent) {
                try {
                    await policy.validateEmpty?.(undefined);
                } catch (cause) {
                    throw new ApiError("invalid-response", { status: response.status, cause });
                }
            } else if (cookieAction && policy.validateEmpty) {
                // 쿠키 요청은 파싱·계약 검증까지 끝난 뒤 탭 간 잠금을 해제한다.
                try {
                    const value: unknown = await response.clone().json();
                    await policy.validateEmpty(value);
                    if (cookieAction !== "logout") {
                        if (
                            typeof value !== "object" ||
                            value === null ||
                            !("accessToken" in value) ||
                            !("expiresIn" in value)
                        ) {
                            throw new ApiError("invalid-response");
                        }
                        assertTokenUsage(value.accessToken, value.expiresIn);
                    }
                } catch (cause) {
                    throw new ApiError("invalid-response", { status: response.status, cause });
                }
            }
            return response;
        };
        return cookieAction ? withAuthCookieLock(cookieAction, execute) : execute();
    },
});

// SDK의 비동기 직렬화보다 먼저 인증 버전을 기록한다.
// 모든 생성 함수가 이 공통 메서드를 사용하므로 도메인별 옵션 추가가 필요 없다.
for (const method of [
    "get",
    "post",
    "put",
    "patch",
    "delete",
    "head",
    "options",
    "trace",
    "connect",
] as const) {
    const execute = client[method];
    Object.defineProperty(client, method, {
        value: (options: Parameters<typeof execute>[0]) => {
            const revision = getAuthRevision();
            const bearer = options.security?.some(
                (security) => security.type === "http" && security.scheme === "bearer",
            );
            const policy: RequestPolicy = {
                auth: bearer ? "required" : "none",
                revision,
                noContent: false,
                validateEmpty: options.responseValidator,
            };
            contexts.set(policy, policy);
            const originalMeta: unknown = Reflect.get(options, "meta");
            const prepared = {
                ...options,
                meta: {
                    ...(typeof originalMeta === "object" && originalMeta !== null
                        ? originalMeta
                        : {}),
                    requestPolicy: policy,
                },
            };
            const pending: Promise<Awaited<ReturnType<typeof execute>>> = execute(prepared);
            return waitForSharedRequest(pending, options.signal).then((value) => {
                // 생성 응답 검증이 끝난 시점에도 이전 계정의 결과를 전달하지 않는다.
                if (options.signal?.aborted || (bearer && revision !== getAuthRevision())) {
                    throw new ApiError("aborted", { cause: options.signal?.reason });
                }
                return policy.noContent ? undefined : value;
            });
        },
    });
}

client.interceptors.request.use((request, options) => {
    const meta: unknown = Reflect.get(options, "meta");
    if (typeof meta !== "object" || meta === null || !("requestPolicy" in meta)) {
        throw new ApiError("invalid-response");
    }
    // SDK 준비 전에 만든 문맥을 그대로 유지해 응답 유무와 인증 버전을 함께 확인한다.
    const policy = meta.requestPolicy;
    if (typeof policy !== "object" || policy === null) {
        throw new ApiError("invalid-response");
    }
    const context = contexts.get(policy);
    if (!context) throw new ApiError("invalid-response");
    policies.set(request, context);
    return request;
});

client.interceptors.error.use((error, response, request) => {
    if (error instanceof ApiError) return error;
    if (request?.signal.aborted) return new ApiError("aborted", { cause: error });
    return response
        ? new ApiError("invalid-response", { status: response.status, cause: error })
        : new ApiError("network", { cause: error });
});
