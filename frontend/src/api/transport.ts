import { ApiError, toApiProblem } from "./apiError.ts";

export type RequestOptions = Omit<RequestInit, "credentials">;

// CSRF 준비 요청도 사용하는 실제 전송 계층. 인증 정책은 http.ts에서 적용한다.
export async function sendResponse(
    path: `/api/${string}`,
    options: RequestOptions = {},
): Promise<Response> {
    const headers = new Headers(options.headers);
    if (!headers.has("Accept")) {
        headers.set("Accept", "application/json, application/problem+json");
    }

    let response: Response;
    let body: string;

    try {
        response = await fetch(path, { ...options, headers, credentials: "same-origin" });
        // 전체 본문 수신 후 인증 버전을 확인할 수 있게 원본 응답을 보존하며 읽는다.
        body = await response.clone().text();
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
        response.status !== 204 &&
        response.status !== 205 &&
        options.method?.toUpperCase() !== "HEAD"
    ) {
        const contentType = response.headers
            .get("Content-Type")
            ?.split(";")[0]
            ?.trim()
            .toLowerCase();
        if (contentType !== "application/json" && !contentType?.endsWith("+json")) {
            throw new ApiError("invalid-response", { status: response.status });
        }
        // 본문 없는 성공은 204·205·HEAD로 구분하고, JSON 응답의 빈 본문은 거절한다.
        if (!body.trim() || response.headers.get("Content-Length") === "0") {
            throw new ApiError("invalid-response", { status: response.status });
        }
    }
    return response;
}

export async function sendRequest(
    path: `/api/${string}`,
    options: RequestOptions = {},
): Promise<unknown> {
    const response = await sendResponse(path, options);

    if (
        response.status === 204 ||
        response.status === 205 ||
        options.method?.toUpperCase() === "HEAD"
    ) {
        return undefined;
    }

    try {
        const value: unknown = JSON.parse(await response.text());
        return value;
    } catch (cause) {
        throw new ApiError("invalid-response", { status: response.status, cause });
    }
}
