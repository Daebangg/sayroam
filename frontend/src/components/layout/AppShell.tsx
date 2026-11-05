import { useEffect, useState, type ReactNode } from "react";
import { ProfileMenu } from "@/components/features/profile/ProfileMenu";
import { AppearancePreview } from "@/components/features/profile/AppearancePreview";
import { useTranslation } from "react-i18next";
import { Brand, Button, Icon, Tooltip } from "@/components/ui";
import { previewNavigation, type PreviewTheme, type PreviewView } from "@/data/preview";

type AppShellProps = {
    view: PreviewView;
    theme: PreviewTheme;
    onViewChange: (view: PreviewView) => void;
    onThemeChange: () => void;
    onLanguageChange: () => void;
    onOpenAuth: () => void;
    children: ReactNode;
};

export function AppShell({
    view,
    theme,
    onViewChange,
    onThemeChange,
    onLanguageChange,
    onOpenAuth,
    children,
}: AppShellProps) {
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
            <div className={`app-shell ${view === "plaza" ? "app-shell--home" : ""}`}>
                <header className="site-header">
                    <Button
                        variant="plain"
                        className="brand-button"
                        type="button"
                        onClick={() => onViewChange("plaza")}
                        aria-label={t("navigation.home")}
                    >
                        <Brand />
                    </Button>
                    <nav className="desktop-nav" aria-label={t("navigation.main")}>
                        {previewNavigation.map((item) => (
                            <Button
                                variant="plain"
                                key={item.view}
                                type="button"
                                className={`nav-item ${view === item.view ? "is-active" : ""}`}
                                aria-current={view === item.view ? "page" : undefined}
                                onClick={() => onViewChange(item.view)}
                            >
                                <Icon name={item.icon} />
                                <span>{t(item.label)}</span>
                            </Button>
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
                                onClick={onLanguageChange}
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
                                onClick={onThemeChange}
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
                            onBrand={() => onViewChange("brand")}
                            onSettings={() => setSettingsOpen(true)}
                        />
                    </div>
                </header>
                {children}
                <nav className="mobile-nav" aria-label={t("navigation.main")}>
                    {previewNavigation.map((item) => (
                        <Button
                            variant="plain"
                            key={item.view}
                            type="button"
                            className={`mobile-nav__item ${view === item.view ? "is-active" : ""}`}
                            aria-label={t(item.label)}
                            aria-current={view === item.view ? "page" : undefined}
                            onClick={() => onViewChange(item.view)}
                        >
                            <Icon name={item.icon} />
                        </Button>
                    ))}
                    <ProfileMenu
                        mobile
                        onLogin={onOpenAuth}
                        onBrand={() => onViewChange("brand")}
                        onSettings={() => setSettingsOpen(true)}
                    />
                </nav>
                <AppearancePreview
                    open={settingsOpen}
                    onClose={() => setSettingsOpen(false)}
                    theme={theme}
                    onThemeChange={onThemeChange}
                    onLanguageChange={onLanguageChange}
                />
            </div>
        </div>
    );
}
