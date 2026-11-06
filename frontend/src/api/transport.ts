import { ApiError, toApiProblem } from "./apiError.ts";

export type RequestOptions = Omit<RequestInit, "credentials">;

// CSRF 준비 요청도 사용하는 실제 전송 계층. 인증 정책은 http.ts에서 적용한다.
export async function sendRequest(
    path: `/api/${string}`,
    options: RequestOptions = {},
): Promise<unknown> {
    const headers = new Headers(options.headers);
    if (!headers.has("Accept")) {
        headers.set("Accept", "application/json, application/problem+json");
    }

    let response: Response;
    let body: string;

    try {
        response = await fetch(path, { ...options, headers, credentials: "same-origin" });
        body = await response.text();
    } catch (cause) {
        const aborted =
            options.signal?.aborted ||
            (cause instanceof DOMException && cause.name === "AbortError");
        throw new ApiError(aborted ? "aborted" : "network", { cause });
    }

    if (!response.ok) {
        let value: unknown;
        try {
            value = JSON.parse(body);
        } catch {
            value = undefined;
        }
        throw new ApiError("http", { problem: toApiProblem(response, value) });
    }

    if (
        response.status === 204 ||
        response.status === 205 ||
        options.method?.toUpperCase() === "HEAD"
    ) {
        return undefined;
    }

    const contentType = response.headers.get("Content-Type")?.split(";")[0]?.trim().toLowerCase();
    if (contentType !== "application/json" && !contentType?.endsWith("+json")) {
        throw new ApiError("invalid-response", { status: response.status });
    }

    try {
        const value: unknown = JSON.parse(body);
        return value;
    } catch (cause) {
        throw new ApiError("invalid-response", { status: response.status, cause });
    }
}
