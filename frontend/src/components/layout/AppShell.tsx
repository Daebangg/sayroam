import { useEffect, useState, type ReactNode } from "react";
import { ProfileMenu } from "@/components/features/profile/ProfileMenu";
import { AppearancePreview } from "@/components/features/profile/AppearancePreview";
import { useTranslation } from "react-i18next";
import { Brand, Button, Icon, Tooltip } from "@/components/ui";
import { appNavigation } from "@/data/preview";
import { Link, NavLink, useNavigate } from "react-router";
import type { ResolvedTheme, ThemePreference } from "@/hooks/useTheme";
import type { Language } from "@/i18n/locale";

type AppShellProps = {
    isHome: boolean;
    theme: ResolvedTheme;
    themePreference: ThemePreference;
    onThemeChange: (preference: ThemePreference) => void;
    onLanguageChange: (language: Language) => void;
    onOpenAuth: () => void;
    children: ReactNode;
};

export function AppShell({
    isHome,
    theme,
    themePreference,
    onThemeChange,
    onLanguageChange,
    onOpenAuth,
    children,
}: AppShellProps) {
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();
    const [settingsOpen, setSettingsOpen] = useState(false);

    useEffect(() => {
        const shortcut = (event: KeyboardEvent) => {
            if (
                event.target instanceof HTMLElement &&
                (event.target.isContentEditable ||
                    event.target.closest("input, textarea, select, dialog"))
            )
                return;
            if ((event.metaKey || event.ctrlKey) && event.key === "," && !event.isComposing) {
                event.preventDefault();
                setSettingsOpen(true);
            }
        };
        window.addEventListener("keydown", shortcut);
        return () => window.removeEventListener("keydown", shortcut);
    }, []);

    return (
        <div
            className={`app-theme app-theme--${theme} ${i18n.resolvedLanguage === "en" ? "is-english" : ""}`}
        >
            <a className="skip-link" href="#main-content">
                {t("navigation.skip")}
            </a>
            <div className={`app-shell ${isHome ? "app-shell--home" : ""}`}>
                <header className="site-header">
                    <Link className="brand-button" to="/" aria-label={t("navigation.home")}>
                        <Brand />
                    </Link>
                    <nav className="desktop-nav" aria-label={t("navigation.main")}>
                        {appNavigation.map((item) => (
                            <NavLink
                                key={item.to}
                                to={item.to}
                                end={item.to === "/"}
                                className={({ isActive }) =>
                                    `nav-item ${isActive ? "is-active" : ""}`
                                }
                            >
                                <Icon name={item.icon} />
                                <span>{t(item.label)}</span>
                            </NavLink>
                        ))}
                    </nav>
                    <div className="header-actions">
                        <Tooltip content={t("common.search")}>
                            <Button
                                variant="ghost"
                                className="icon-button desktop-only"
                                aria-label={t("common.search")}
                            >
                                <Icon name="search" />
                            </Button>
                        </Tooltip>
                        <Tooltip content={t("common.changeLanguage")}>
                            <Button
                                variant="ghost"
                                className="language-button desktop-only"
                                onClick={() =>
                                    onLanguageChange(i18n.resolvedLanguage === "ko" ? "en" : "ko")
                                }
                                aria-label={t("common.changeLanguage")}
                            >
                                {i18n.resolvedLanguage === "ko"
                                    ? t("common.englishShort")
                                    : t("common.koreanShort")}
                            </Button>
                        </Tooltip>
                        <Tooltip
                            content={t(theme === "light" ? "common.darkMode" : "common.lightMode")}
                        >
                            <Button
                                variant="ghost"
                                className="icon-button"
                                onClick={() => onThemeChange(theme === "light" ? "dark" : "light")}
                                aria-label={t(
                                    theme === "light" ? "common.darkMode" : "common.lightMode",
                                )}
                            >
                                <Icon name={theme === "light" ? "moon" : "sun"} />
                            </Button>
                        </Tooltip>
                        <Tooltip content={t("common.notifications")}>
                            <Button
                                variant="ghost"
                                className="icon-button mobile-only"
                                aria-label={t("common.notifications")}
                            >
                                <Icon name="bell" />
                            </Button>
                        </Tooltip>
                        <ProfileMenu
                            onLogin={onOpenAuth}
                            onBrand={() => void navigate("/brand")}
                            onSettings={() => setSettingsOpen(true)}
                        />
                    </div>
                </header>
                {children}
                <nav className="mobile-nav" aria-label={t("navigation.main")}>
                    {appNavigation.map((item) => (
                        <NavLink
                            key={item.to}
                            to={item.to}
                            end={item.to === "/"}
                            className={({ isActive }) =>
                                `mobile-nav__item ${isActive ? "is-active" : ""}`
                            }
                            aria-label={t(item.label)}
                        >
                            <Icon name={item.icon} />
                        </NavLink>
                    ))}
                    <ProfileMenu
                        mobile
                        onLogin={onOpenAuth}
                        onBrand={() => void navigate("/brand")}
                        onSettings={() => setSettingsOpen(true)}
                    />
                </nav>
                <AppearancePreview
                    open={settingsOpen}
                    onClose={() => setSettingsOpen(false)}
                    theme={themePreference}
                    onThemeChange={onThemeChange}
                    onLanguageChange={onLanguageChange}
                />
            </div>
        </div>
    );
}
