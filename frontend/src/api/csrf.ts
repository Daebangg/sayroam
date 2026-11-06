import { ApiError } from "./apiError.ts";
import { sendRequest } from "./transport.ts";

const cookiePrefix = "XSRF-TOKEN=";
const safeMethods = new Set(["GET", "HEAD", "OPTIONS", "TRACE"]);
let preparation: Promise<void> | undefined;

export function requiresCsrf(path: `/api/${string}`, method = "GET"): boolean {
    const pathname = path.split(/[?#]/)[0] ?? path;
    // 백엔드 SecurityConfig의 requireCsrfProtectionMatcher와 같은 적용 범위.
    return (
        !safeMethods.has(method.toUpperCase()) &&
        (pathname === "/api/auth" || pathname.startsWith("/api/auth/"))
    );
}

export function readCsrfToken(): string | undefined {
    const matches = document.cookie
        .split(";")
        .map((cookie) => cookie.trim())
        .filter((cookie) => cookie.startsWith(cookiePrefix));

    if (matches.length !== 1) return undefined;
    return matches[0]?.slice(cookiePrefix.length) || undefined;
}

export async function ensureCsrfToken(): Promise<string> {
    const existing = readCsrfToken();
    if (existing) return existing;

    if (!preparation) {
        preparation = sendRequest("/api/auth/csrf", { cache: "no-store" })
            .then(() => undefined)
            .finally(() => {
                preparation = undefined;
            });
    }

    // 한 호출의 취소로 다른 호출도 기다리는 공유 준비 요청을 중단하지 않는다.
    await preparation;

    const token = readCsrfToken();
    if (!token) {
        throw new ApiError("invalid-response", { status: 204 });
    }
    return token;
}
