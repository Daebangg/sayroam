import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, ScrollRestoration, useLocation, useNavigate, useNavigation } from "react-router";
import { AppShell } from "@/components/layout/AppShell";
import { AuthPreview } from "@/components/features/auth/AuthPreview";
import { setLanguage } from "@/i18n";
import type { Language } from "@/i18n/locale";
import { useTheme, type ThemePreference } from "@/hooks/useTheme";
import { LoadingPage } from "@/pages/status/StatusPage";
import { RouteAccessibility } from "@/routing/RouteAccessibility";

export default function App() {
    const { i18n } = useTranslation();
    const location = useLocation();
    const navigate = useNavigate();
    const navigation = useNavigation();
    const theme = useTheme();
    const [authOpen, setAuthOpen] = useState(
        () => new URLSearchParams(window.location.search).get("modal") === "auth",
    );

    function syncPreviewUrl(): void {
        const url = window.location;
        if (location.search !== url.search) {
            void navigate(url.pathname + url.search + url.hash, {
                replace: true,
                preventScrollReset: true,
            });
        }
    }
    function changeLanguage(language: Language): void {
        setLanguage(language);
        syncPreviewUrl();
    }
    function changeTheme(preference: ThemePreference): void {
        theme.setPreference(preference);
        syncPreviewUrl();
    }

    return (
        <AppShell
            isHome={location.pathname === "/"}
            theme={theme.resolved}
            themePreference={theme.preference}
            onThemeChange={changeTheme}
            onLanguageChange={changeLanguage}
            onOpenAuth={() => setAuthOpen(true)}
        >
            {navigation.state !== "idle" ? (
                <LoadingPage />
            ) : (
                <>
                    <Outlet
                        context={{
                            onOpenAuth: () => setAuthOpen(true),
                            onLanguageChange: () =>
                                changeLanguage(i18n.resolvedLanguage === "ko" ? "en" : "ko"),
                        }}
                    />
                    <RouteAccessibility />
                </>
            )}
            <ScrollRestoration />
            <AuthPreview open={authOpen} onClose={() => setAuthOpen(false)} />
        </AppShell>
    );
}
