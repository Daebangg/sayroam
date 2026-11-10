import type { AuthState } from "@/hooks/auth.types";
import type { ThemePreference } from "@/hooks/useTheme";
import type { Language } from "@/i18n/locale";
import type { AccountResult } from "@/hooks/account.types";

export type PageContext = {
    onOpenAuth: () => void;
    onLanguageChange: () => void;
    member: AuthState["user"];
    themePreference: ThemePreference;
    onThemeChange: (preference: ThemePreference) => void;
    onSelectLanguage: (language: Language) => void;
    onUpdateNickname: (nickname: string, signal: AbortSignal) => Promise<void>;
    onAccountResult: (result: AccountResult) => void;
};
