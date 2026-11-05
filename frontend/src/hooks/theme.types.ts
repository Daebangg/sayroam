export type ThemePreference = "system" | "light" | "dark";
export type ResolvedTheme = "light" | "dark";
export type ThemeSnapshot = Readonly<{ preference: ThemePreference; resolved: ResolvedTheme }>;
export type ThemeController = {
    getSnapshot: () => ThemeSnapshot;
    subscribe: (listener: () => void) => () => void;
    setPreference: (preference: ThemePreference) => void;
};
