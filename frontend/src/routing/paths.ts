export const pagePaths = {
    plaza: "/",
    trips: "/trips",
    matching: "/matching",
    rooms: "/rooms",
    brand: "/brand",
    settings: "/settings",
    appearance: "/settings/appearance",
    account: "/settings/account",
    profile: "/settings/profile",
} as const;

// 같은 출처의 현재 주소를 유지하므로 로그인 복귀용 외부 URL을 받지 않는다.
export function isMemberPath(pathname: string): boolean {
    const path = pathname.replace(/\/+$/, "").toLowerCase();
    return [pagePaths.trips, pagePaths.rooms, pagePaths.account, pagePaths.profile].some(
        (memberPath) => path === memberPath,
    );
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
