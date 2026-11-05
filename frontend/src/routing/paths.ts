export const pagePaths = {
    plaza: "/",
    trips: "/trips",
    matching: "/matching",
    rooms: "/rooms",
    brand: "/brand",
} as const;

export function legacyPageUrl(url: URL): string | null {
    if (url.pathname !== "/") return null;
    const view = url.searchParams.get("view");
    if (!view || !Object.hasOwn(pagePaths, view)) return null;
    const result = new URL(url);
    result.pathname = pagePaths[view as keyof typeof pagePaths];
    result.searchParams.delete("view");
    return result.pathname + result.search + result.hash;
}
