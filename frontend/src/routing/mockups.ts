export const mockupScreens = [
    "welcome",
    "onboarding",
    "home",
    "trips",
    "matching",
    "rooms",
] as const;
export type MockupScreen = (typeof mockupScreens)[number];

export function getMockupScreen(pathname: string, search: string): MockupScreen | null {
    if (pathname.replace(/\/+$/, "").toLowerCase() !== "/brand") return null;
    const value = new URLSearchParams(search).get("preview");
    return mockupScreens.find((screen) => screen === value) ?? null;
}

export function mockupUrl(screen: MockupScreen): string {
    return `/brand?preview=${screen}`;
}
