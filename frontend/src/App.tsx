import { useState } from "react";
import { useTranslation } from "react-i18next";
import { AppShell } from "@/components/layout/AppShell";
import { AuthPreview } from "@/components/features/auth/AuthPreview";
import type { PreviewTheme, PreviewView } from "@/data/preview";
import { HomePage } from "@/pages/home/HomePage";
import { TripsPage } from "@/pages/trips/TripsPage";
import { MatchingPage } from "@/pages/matching/MatchingPage";
import { RoomsPage } from "@/pages/rooms/RoomsPage";
import { BrandPage } from "@/pages/brand/BrandPage";

function initialView(): PreviewView {
    switch (new URLSearchParams(window.location.search).get("view")) {
        case "trips":
            return "trips";
        case "matching":
            return "matching";
        case "rooms":
            return "rooms";
        case "brand":
            return "brand";
        default:
            return "plaza";
    }
}

export default function App() {
    const { i18n } = useTranslation();
    const [view, setView] = useState<PreviewView>(initialView);
    const [theme, setTheme] = useState<PreviewTheme>(() =>
        new URLSearchParams(window.location.search).get("theme") === "dark" ? "dark" : "light",
    );
    const [authOpen, setAuthOpen] = useState(
        () => new URLSearchParams(window.location.search).get("modal") === "auth",
    );

    function changeLanguage(): void {
        void i18n.changeLanguage(i18n.resolvedLanguage === "ko" ? "en" : "ko");
    }

    return (
        <AppShell
            view={view}
            theme={theme}
            onViewChange={setView}
            onThemeChange={() => setTheme(theme === "light" ? "dark" : "light")}
            onLanguageChange={changeLanguage}
            onOpenAuth={() => setAuthOpen(true)}
        >
            {view === "plaza" && (
                <HomePage onStart={() => setAuthOpen(true)} onOpenBrand={() => setView("brand")} />
            )}
            {view === "trips" && <TripsPage />}
            {view === "matching" && <MatchingPage />}
            {view === "rooms" && <RoomsPage />}
            {view === "brand" && (
                <BrandPage onOpenAuth={() => setAuthOpen(true)} onLanguageChange={changeLanguage} />
            )}
            <AuthPreview open={authOpen} onClose={() => setAuthOpen(false)} />
        </AppShell>
    );
}
