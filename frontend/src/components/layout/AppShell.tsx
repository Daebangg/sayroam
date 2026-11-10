import { useEffect, useState, type ReactNode } from "react";
import { ProfileMenu } from "@/components/features/profile/ProfileMenu";
import { useTranslation } from "react-i18next";
import { Brand, Button, Icon, Tooltip } from "@/components/ui";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { mockupUrl, type MockupScreen } from "@/routing/mockups";
import { appNavigation } from "@/data/preview";
import { pagePaths } from "@/routing/paths";
import { Link, NavLink, useNavigate, useLocation, useNavigation } from "react-router";
import type { ResolvedTheme } from "@/hooks/useTheme";

type AppShellProps = {
    mockupScreen: MockupScreen | null;
    isHome: boolean;
    theme: ResolvedTheme;
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
    onOpenAuth,
    member,
    authBusy,
    onLogout,
    children,
}: AppShellProps) {
    const navigate = useNavigate();
    const navigation = useNavigation();
    const { t, i18n } = useTranslation();
    const [notificationsOpen, setNotificationsOpen] = useState(false);
    const { pathname } = useLocation();
    const inSettings = /^\/settings(?:\/|$)/i.test(pathname);

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
            if (
                (event.metaKey || event.ctrlKey) &&
                event.shiftKey &&
                event.key.toLowerCase() === "p" &&
                !event.isComposing
            ) {
                event.preventDefault();
                void navigate(pagePaths.profile);
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
                {navigation.state !== "idle" && (
                    <div className="route-progress" role="status">
                        <span className="visually-hidden">{t("pageState.loading.title")}</span>
                    </div>
                )}
                <header className={`site-header ${inSettings ? "site-header--settings" : ""}`}>
                    <Link
                        className="brand-button"
                        to={mockupScreen ? mockupUrl("home") : "/"}
                        aria-label={t("navigation.home")}
                    >
                        <Brand />
                    </Link>
                    {!inSettings && (
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
                    )}
                    <div className="header-actions">
                        {member && (
                            <Tooltip content={t("common.notifications")}>
                                <Button
                                    variant="ghost"
                                    className="icon-button notification-trigger"
                                    aria-label={t("common.notifications")}
                                    onClick={() => setNotificationsOpen(true)}
                                >
                                    <Icon name="bell" />
                                </Button>
                            </Tooltip>
                        )}
                        <ProfileMenu
                            member={member}
                            profileImageUrl={member?.profileImageUrl ?? null}
                            busy={authBusy}
                            onLogout={onLogout}
                            onLogin={onOpenAuth}
                            onBrand={() => void navigate("/brand")}
                            onSettings={() => void navigate(pagePaths.settings)}
                            onProfile={() => void navigate(pagePaths.profile)}
                        />
                    </div>
                </header>
                {children}
                {!inSettings && (
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
                            onProfile={() => void navigate(pagePaths.profile)}
                        />
                    </nav>
                )}
                <PreviewDialog
                    open={member !== null && notificationsOpen}
                    title={t("common.notifications")}
                    description={t("mockup.notificationsEmpty")}
                    scene="island"
                    onClose={() => setNotificationsOpen(false)}
                />
            </div>
        </div>
    );
}
