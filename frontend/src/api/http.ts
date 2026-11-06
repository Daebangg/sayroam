import { ApiError } from "./apiError.ts";
import { ensureCsrfToken, requiresCsrf } from "./csrf.ts";
import { sendRequest, type RequestOptions } from "./transport.ts";

export type { RequestOptions } from "./transport.ts";

export async function request(
    path: `/api/${string}`,
    options: RequestOptions = {},
): Promise<unknown> {
    if (options.signal?.aborted) {
        throw new ApiError("aborted", { cause: options.signal.reason });
    }

    if (requiresCsrf(path, options.method)) {
        const headers = new Headers(options.headers);
        headers.set("X-XSRF-TOKEN", await ensureCsrfToken());
        return sendRequest(path, { ...options, headers });
    }

    return sendRequest(path, options);
}
