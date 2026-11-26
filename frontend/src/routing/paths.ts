export const pagePaths = {
    plaza: "/",
    trips: "/trips",
    matching: "/matching",
    rooms: "/rooms",
    join: "/join",
    brand: "/brand",
    settings: "/settings",
    keyboard: "/settings/keyboard",
    appearance: "/settings/appearance",
    account: "/settings/account",
    profile: "/settings/profile",
} as const;

export function tripDetailPath(tripId: number): string {
    return `${pagePaths.trips}/${tripId}`;
}

// 초대 코드는 서버 로그·Referer에 남지 않도록 경로나 쿼리 대신 해시에 둔다.
export function invitationUrl(origin: string, code: string): string {
    return `${origin}${pagePaths.join}#code=${encodeURIComponent(code)}`;
}

export function invitationCode(hash: string): string | null {
    const code = new URLSearchParams(hash.replace(/^#/, "")).get("code")?.trim();
    return code ? code : null;
}

// 같은 출처의 현재 주소를 유지하므로 로그인 복귀용 외부 URL을 받지 않는다.
export function isMemberPath(pathname: string): boolean {
    const path = pathname.replace(/\/+$/, "").toLowerCase();
    if (/^\/trips\/[1-9]\d{0,15}$/.test(path)) return true;
    return [
        pagePaths.trips,
        pagePaths.matching,
        pagePaths.rooms,
        pagePaths.join,
        pagePaths.account,
        pagePaths.profile,
        pagePaths.keyboard,
    ].some((memberPath) => path === memberPath);
}

export function legacyPageUrl(url: URL): string | null {
    if (url.pathname !== "/") return null;
    const view = url.searchParams.get("view");
    if (!view || !Object.hasOwn(pagePaths, view)) return null;
    const result = new URL(url);
    result.pathname = pagePaths[view as keyof typeof pagePaths];
    result.searchParams.delete("view");
    return result.pathname + result.search + result.hash;
}

export function companionRequestUrl(requestId: number): string {
    return `${pagePaths.matching}?requestId=${requestId}`;
}

export function recruitmentResultsUrl(searchId: number): string {
    return `${pagePaths.matching}?searchId=${searchId}`;
}

// 알림으로 같은 매칭 결과를 연속해서 열면 이전 결과 화면들을 건너뛴다.
export function matchingNotificationState(location: {
    pathname: string;
    search: string;
    state: unknown;
}) {
    const params = new URLSearchParams(location.search);
    const isResults =
        (location.pathname === pagePaths.matching &&
            params.has("searchId") &&
            params.get("view") !== "requests") ||
        (location.pathname === pagePaths.brand &&
            ["matching-found", "matching-searching", "matching-empty", "matching-delayed"].includes(
                params.get("preview") ?? "",
            ));
    const state = location.state && typeof location.state === "object" ? location.state : {};
    const prior =
        "resultsBackSteps" in state &&
        typeof state.resultsBackSteps === "number" &&
        Number.isSafeInteger(state.resultsBackSteps) &&
        state.resultsBackSteps > 0
            ? state.resultsBackSteps
            : 1;
    return { resultsBackSteps: isResults ? prior + 1 : 1, fromMatchingMain: true };
}
