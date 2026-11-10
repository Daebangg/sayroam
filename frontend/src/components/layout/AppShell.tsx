import { useEffect, useId, useState, type ReactNode } from "react";
import { ProfileMenu } from "@/components/features/profile/ProfileMenu";
import { useTranslation } from "react-i18next";
import { Brand, Button, Icon, Tooltip, Modal, TextField } from "@/components/ui";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { mockupUrl, type MockupScreen } from "@/routing/mockups";
import { appNavigation } from "@/data/preview";
import { pagePaths } from "@/routing/paths";
import { Link, NavLink, useNavigate } from "react-router";
import type { ResolvedTheme, ThemePreference } from "@/hooks/useTheme";
import type { Language } from "@/i18n/locale";

type AppShellProps = {
    mockupScreen: MockupScreen | null;
    isHome: boolean;
    theme: ResolvedTheme;
    onThemeChange: (preference: ThemePreference) => void;
    onLanguageChange: (language: Language) => void;
    onOpenAuth: () => void;
    member: { nickname: string; email: string | null; profileImageUrl?: string | null } | null;
    authBusy: boolean;
    onLogout: () => void;
    children: ReactNode;
};

export function AppShell({
    mockupScreen,
    isHome,
    theme,
    onThemeChange,
    onLanguageChange,
    onOpenAuth,
    member,
    authBusy,
    onLogout,
    children,
}: AppShellProps) {
    const navigate = useNavigate();
    const { t, i18n } = useTranslation();
    const [utility, setUtility] = useState<"search" | "notifications" | null>(null);
    const [query, setQuery] = useState("");
    const searchId = useId();

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
                void navigate(pagePaths.settings);
            }
        };
        window.addEventListener("keydown", shortcut);
        return () => window.removeEventListener("keydown", shortcut);
    }, [navigate]);

    return (
        <div
            className={`app-theme app-theme--${theme} ${i18n.resolvedLanguage === "en" ? "is-english" : ""}`}
        >
            <a className="skip-link" href="#main-content">
                {t("navigation.skip")}
            </a>
            <div
                className={`app-shell ${isHome ? "app-shell--home" : ""} ${mockupScreen === "welcome" || mockupScreen === "onboarding" ? "app-shell--intro" : ""}`}
            >
                <header className="site-header">
                    <Link
                        className="brand-button"
                        to={mockupScreen ? mockupUrl("home") : "/"}
                        aria-label={t("navigation.home")}
                    >
                        <Brand />
                    </Link>
                    <nav className="desktop-nav" aria-label={t("navigation.main")}>
                        {appNavigation.map((item) => (
                            <NavLink
                                key={item.to}
                                to={mockupScreen ? mockupUrl(item.icon) : item.to}
                                end={item.to === "/"}
                                aria-current={
                                    mockupScreen
                                        ? mockupScreen === item.icon
                                            ? "page"
                                            : false
                                        : undefined
                                }
                                className={({ isActive }) =>
                                    `nav-item ${(mockupScreen ? mockupScreen === item.icon : isActive) ? "is-active" : ""}`
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
                                onClick={() => setUtility("search")}
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
                                className="icon-button notification-trigger"
                                aria-label={t("common.notifications")}
                                onClick={() => setUtility("notifications")}
                            >
                                <Icon name="bell" />
                            </Button>
                        </Tooltip>
                        <ProfileMenu
                            member={member}
                            profileImageUrl={member?.profileImageUrl ?? null}
                            busy={authBusy}
                            onLogout={onLogout}
                            onLogin={onOpenAuth}
                            onBrand={() => void navigate("/brand")}
                            onSettings={() => void navigate(pagePaths.settings)}
                        />
                    </div>
                </header>
                {children}
                <nav className="mobile-nav" aria-label={t("navigation.main")}>
                    {appNavigation.map((item) => (
                        <NavLink
                            key={item.to}
                            to={mockupScreen ? mockupUrl(item.icon) : item.to}
                            end={item.to === "/"}
                            aria-current={
                                mockupScreen
                                    ? mockupScreen === item.icon
                                        ? "page"
                                        : false
                                    : undefined
                            }
                            className={({ isActive }) =>
                                `mobile-nav__item ${(mockupScreen ? mockupScreen === item.icon : isActive) ? "is-active" : ""}`
                            }
                            aria-label={t(item.label)}
                        >
                            <Icon name={item.icon} />
                        </NavLink>
                    ))}
                    <ProfileMenu
                        member={member}
                        profileImageUrl={member?.profileImageUrl ?? null}
                        busy={authBusy}
                        onLogout={onLogout}
                        mobile
                        onLogin={onOpenAuth}
                        onBrand={() => void navigate("/brand")}
                        onSettings={() => void navigate(pagePaths.settings)}
                    />
                </nav>
                <PreviewDialog
                    open={utility === "notifications"}
                    title={t("common.notifications")}
                    description={t("mockup.notificationsEmpty")}
                    scene="island"
                    onClose={() => setUtility(null)}
                />
                <Modal
                    open={utility === "search"}
                    onClose={() => setUtility(null)}
                    labelledBy={searchId}
                    className="ui-modal--showcase"
                >
                    <Button
                        variant="ghost"
                        className="icon-button ui-modal__close"
                        onClick={() => setUtility(null)}
                        aria-label={t("common.close")}
                    >
                        <Icon name="close" variant="outline" />
                    </Button>
                    <h2 id={searchId}>{t("common.search")}</h2>
                    <form
                        className="utility-search"
                        onSubmit={(event) => {
                            event.preventDefault();
                            setUtility(null);
                            void navigate(
                                (mockupScreen ? mockupUrl("trips") + "&" : "/trips?") +
                                    "q=" +
                                    encodeURIComponent(query.trim()),
                            );
                        }}
                    >
                        <TextField
                            label={t("mockup.destination")}
                            value={query}
                            onChange={(event) => setQuery(event.target.value)}
                            maxLength={50}
                            placeholder={t("mockup.destinationPlaceholder")}
                        />
                        <Button type="submit">
                            <Icon name="search" variant="outline" />
                            {t("common.search")}
                        </Button>
                    </form>
                </Modal>
            </div>
        </div>
    );
}
