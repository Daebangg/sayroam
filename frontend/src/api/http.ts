import createClient, { type Client } from "openapi-fetch";
import { ApiError } from "./apiError.ts";
import { waitForSharedRequest } from "./abort.ts";
import { refreshApi } from "./auth/authApi.ts";
import { invalidateOtherTabs, withAuthCookieLock } from "./auth/cookieCoordination.ts";
import { refreshAccessToken } from "./auth/refreshSession.ts";
import {
    clearAccessToken,
    getAccessTokenSnapshot,
    getAuthRevision,
    type AccessTokenSnapshot,
} from "./auth/tokenStore.ts";
import { ensureCsrfToken, requiresCsrf } from "./csrf.ts";
import type { paths } from "./generated/schema.ts";
import { sendResponse, type RequestOptions as TransportOptions } from "./transport.ts";

type PolicyOptions = TransportOptions & { auth?: "none" | "required" };
export type RequestOptions = Pick<TransportOptions, "signal"> & {
    auth?: "none" | "required";
    cookieAction?: "login" | "refresh" | "logout";
};

function isApiPath(path: string): path is `/api/${string}` {
    return path.startsWith("/api/");
}

function assertCurrentRequest(
    snapshot: AccessTokenSnapshot | undefined,
    signal: AbortSignal | null | undefined,
): void {
    if (signal?.aborted || (snapshot && snapshot.revision !== getAuthRevision())) {
        throw new ApiError("aborted", { cause: signal?.reason });
    }
}

export async function withRequestPolicy<T>(
    path: `/api/${string}`,
    options: PolicyOptions,
    execute: (path: `/api/${string}`, options: TransportOptions) => Promise<T>,
    recover?: (revision: number) => Promise<AccessTokenSnapshot>,
): Promise<T> {
    const { auth = "none", ...transportOptions } = options;
    let snapshot = auth === "required" ? getAccessTokenSnapshot() : undefined;
    assertCurrentRequest(snapshot, options.signal);
    if (auth === "required" && !snapshot) throw new ApiError("authentication-required");

    const headers = new Headers(options.headers);
    if (snapshot) headers.set("Authorization", `Bearer ${snapshot.value}`);
    else headers.delete("Authorization");

    if (requiresCsrf(path, options.method)) {
        headers.set("X-XSRF-TOKEN", await waitForSharedRequest(ensureCsrfToken(), options.signal));
    }

    assertCurrentRequest(snapshot, options.signal);
    for (let attempt = 0; ; attempt++) {
        try {
            const value = await execute(path, { ...transportOptions, headers });
            assertCurrentRequest(snapshot, options.signal);
            return value;
        } catch (error) {
            assertCurrentRequest(snapshot, options.signal);
            if (
                !snapshot ||
                !(error instanceof ApiError) ||
                error.kind !== "http" ||
                error.status !== 401
            ) {
                throw error;
            }
            // 변경 요청과 갱신 후에도 거절된 요청은 재전송하지 않는다.
            if (
                !recover ||
                attempt > 0 ||
                !["GET", "HEAD", "OPTIONS"].includes((options.method ?? "GET").toUpperCase())
            ) {
                if (getAccessTokenSnapshot() === snapshot) clearAccessToken();
                throw error;
            }
            // 다른 요청이 이미 갱신했다면 그 토큰을 사용한다. 늦은 401로 재갱신하지 않는다.
            const current = getAccessTokenSnapshot();
            snapshot =
                current !== snapshot && current
                    ? current
                    : await waitForSharedRequest(recover(snapshot.revision), options.signal);
            assertCurrentRequest(snapshot, options.signal);
            headers.set("Authorization", `Bearer ${snapshot.value}`);
        }
    }
}

// 요청마다 인증 버전을 고정한다. 파싱·도메인 검증도 같은 취소·쿠키 잠금 범위에 둔다.
export async function request<T>(
    execute: (client: Client<paths>) => Promise<T>,
    options: RequestOptions = {},
): Promise<T> {
    const revision = getAuthRevision();
    let status: number | undefined;
    const assertCurrent = (): void => {
        if (options.signal?.aborted || revision !== getAuthRevision()) {
            throw new ApiError("aborted", { cause: options.signal?.reason });
        }
    };
    assertCurrent();
    const origin =
        typeof window === "undefined" ? "http://sayroam.invalid" : window.location.origin;
    const client = createClient<paths>({
        baseUrl: origin,
        credentials: "same-origin",
        headers: { Accept: "application/json, application/problem+json" },
        signal: options.signal ?? null,
        fetch: async (input) => {
            assertCurrent();
            const url = new URL(input.url);
            const path = url.pathname + url.search;
            if (url.origin !== origin || !isApiPath(path)) {
                throw new ApiError("invalid-response");
            }
            const transport: TransportOptions = {
                method: input.method,
                headers: input.headers,
                signal: input.signal,
                cache: input.cache,
            };
            if (input.body !== null) transport.body = await input.text();
            assertCurrent();
            const response = await withRequestPolicy(
                path,
                { ...transport, auth: options.auth ?? "none" },
                (path, transport) => {
                    assertCurrent();
                    return sendResponse(path, transport);
                },
                (currentRevision) => refreshAccessToken(refreshApi, currentRevision),
            );
            status = response.status;
            return response;
        },
    });
    const run = async (): Promise<T> => {
        try {
            assertCurrent();
            if (options.cookieAction === "login" || options.cookieAction === "logout") {
                invalidateOtherTabs();
            }
            const value = await execute(client);
            assertCurrent();
            return value;
        } catch (cause) {
            if (cause instanceof ApiError) throw cause;
            assertCurrent();
            throw new ApiError("invalid-response", {
                ...(status === undefined ? {} : { status }),
                cause,
            });
        }
    };
    return waitForSharedRequest(
        options.cookieAction ? withAuthCookieLock(options.cookieAction, run) : run(),
        options.signal,
    );
}
