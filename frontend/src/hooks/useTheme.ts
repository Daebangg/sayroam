import { useSyncExternalStore } from "react";

import type { ThemeController, ThemeSnapshot } from "./theme.types";
export type { ThemePreference, ResolvedTheme } from "./theme.types";

declare global {
    interface Window {
        sayroamTheme: ThemeController;
    }
}

export function useTheme(): ThemeSnapshot & Pick<ThemeController, "setPreference"> {
    const { getSnapshot, subscribe, setPreference } = window.sayroamTheme;
    const theme = useSyncExternalStore(subscribe, getSnapshot);
    return { ...theme, setPreference };
}
