import { getAuthCookieState } from "../api/auth/cookieCoordination.ts";
import { pagePaths } from "./paths.ts";

const key = "sayroam.social.return.v1";
export type SocialReturn = { code: string; returnPath: string };
export function safeSocialReturn(value: unknown): string {
    if (
        typeof value !== "string" ||
        !value.startsWith("/") ||
        value.startsWith("//") ||
        value.includes("\\")
    )
        return "/";
    const url = new URL(value, "https://sayroam.invalid");
    if (!Object.values(pagePaths).some((path) => path === url.pathname.toLowerCase())) return "/";
    url.searchParams.delete("modal");
    if (new URLSearchParams(url.hash.slice(1)).has("social_result")) url.hash = "";
    return url.pathname + url.search + url.hash;
}
export function rememberSocialReturn(): void {
    const location = window.location;
    sessionStorage.setItem(
        key,
        JSON.stringify({
            returnPath: safeSocialReturn(location.pathname + location.search + location.hash),
            createdAt: Date.now(),
            cookieState: getAuthCookieState(),
        }),
    );
}
export function forgetSocialReturn(): void {
    sessionStorage.removeItem(key);
}
export function parseSocialReturn(
    url: URL,
    saved: string | null,
    cookieState: string | null,
    now: number,
): SocialReturn | null {
    const values = new URLSearchParams(url.hash.slice(1));
    if (!values.has("social_result")) return null;
    const failed = { code: "", returnPath: "/" };
    const codes = values.getAll("social_result");
    if (codes.length !== 1 || !/^[A-Za-z0-9_-]{43}$/.test(codes[0]!)) return failed;
    try {
        const context: unknown = JSON.parse(saved ?? "null");
        if (
            typeof context !== "object" ||
            context === null ||
            !("createdAt" in context) ||
            typeof context.createdAt !== "number" ||
            context.createdAt > now ||
            now - context.createdAt > 15 * 60 * 1000 ||
            !("cookieState" in context) ||
            context.cookieState !== cookieState ||
            !("returnPath" in context)
        )
            return failed;
        return { code: codes[0]!, returnPath: safeSocialReturn(context.returnPath) };
    } catch {
        return failed;
    }
}
function capture(): SocialReturn | null {
    if (typeof window === "undefined") return null;
    const url = new URL(window.location.href);
    if (!new URLSearchParams(url.hash.slice(1)).has("social_result")) return null;
    // React Router·효과 실행 전에 비밀 결과를 주소·뒤로가기 기록에서 제거한다.
    window.history.replaceState(window.history.state, "", url.pathname + url.search);
    try {
        const saved = sessionStorage.getItem(key);
        forgetSocialReturn();
        return parseSocialReturn(url, saved, getAuthCookieState(), Date.now());
    } catch {
        return { code: "", returnPath: "/" };
    }
}
export const socialReturn = capture();
