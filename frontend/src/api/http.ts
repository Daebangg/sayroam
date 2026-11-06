import { ApiError } from "./apiError.ts";
import { waitForSharedRequest } from "./abort.ts";
import {
    clearAccessToken,
    getAccessTokenSnapshot,
    getAuthRevision,
    type AccessTokenSnapshot,
} from "./auth/tokenStore.ts";
import { ensureCsrfToken, requiresCsrf } from "./csrf.ts";
import { sendRequest, type RequestOptions as TransportOptions } from "./transport.ts";

export type RequestOptions = TransportOptions & { auth?: "none" | "required" };

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
    options: RequestOptions,
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

export async function request(
    path: `/api/${string}`,
    options: RequestOptions = {},
): Promise<unknown> {
    return withRequestPolicy(path, options, sendRequest);
}
