import { ApiError } from "../apiError.ts";
import { clearAccessToken } from "./tokenStore.ts";

// 토큰을 탭 사이에 전송하거나 저장하지 않는다. 계정 변경·로그아웃만 알린다.
const channel =
    typeof window !== "undefined" && typeof BroadcastChannel !== "undefined"
        ? new BroadcastChannel("sayroam-auth")
        : undefined;
channel?.addEventListener("message", (event: MessageEvent<unknown>) => {
    if (event.data === "invalidate") clearAccessToken();
});

const stateKey = "sayroam.auth.cookie-state.v1";
type CookieAction = "login" | "refresh" | "logout";

if (typeof window !== "undefined") {
    window.addEventListener("storage", (event) => {
        if (event.key === stateKey || event.key === null) clearAccessToken();
    });
}

function readState(): string | null {
    try {
        return localStorage.getItem(stateKey);
    } catch (cause) {
        throw new ApiError("authentication-unavailable", { cause });
    }
}

function writeState(blocked: boolean): void {
    try {
        // 비밀값이 아닌 쿠키 작업의 세대·자동 복원 금지 여부만 공유한다.
        localStorage.setItem(stateKey, `${blocked ? "blocked" : "ready"}:${crypto.randomUUID()}`);
    } catch (cause) {
        throw new ApiError("authentication-unavailable", { cause });
    }
}

export function invalidateOtherTabs(): void {
    channel?.postMessage("invalidate");
}

export async function withAuthCookieLock<T>(
    action: CookieAction,
    execute: () => Promise<T>,
): Promise<T> {
    // Node 테스트에는 쿠키를 공유하는 브라우저 탭이 없다.
    if (typeof window === "undefined") return execute();
    if (!navigator.locks) throw new ApiError("authentication-unavailable");
    const before = readState();
    return navigator.locks.request("sayroam-auth-cookie", async () => {
        const current = readState();
        if (action === "refresh" && (before !== current || current?.startsWith("blocked:"))) {
            throw new ApiError("authentication-required");
        }
        // 명시적인 새 로그인만 실패·로그아웃 이후의 자동 복원 금지를 해제한다.
        if (action !== "refresh") writeState(true);
        try {
            const value = await execute();
            if (action === "login") writeState(false);
            return value;
        } catch (error) {
            // 잠금을 풀기 전에 기록해 다음 탭이 불확실한 쿠키로 재갱신하지 않게 한다.
            writeState(true);
            invalidateOtherTabs();
            throw error;
        }
    });
}
