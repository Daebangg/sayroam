import { useEffect, useId, useRef, useState, type ReactNode } from "react";
import { RoamDiscoveryLandscape } from "@/components/features/matching/RoamDiscoveryLandscape";
import { ChatSidebar, ChatSidebarButton } from "@/components/features/chat/ChatSidebar";
import { ProfileMenu } from "@/components/features/profile/ProfileMenu";
import { useTranslation } from "react-i18next";
import { Brand, Icon, Tooltip } from "@/components/ui";
import { NotificationInboxPreview } from "@/components/features/notifications/NotificationInboxPreview";
import { mockupUrl, type MockupScreen } from "@/routing/mockups";
import { primaryNavigation } from "@/data/preview";
import { pagePaths } from "@/routing/paths";
import { Link, NavLink, useNavigate, useLocation, useMatch, useNavigation } from "react-router";
import type { ResolvedTheme } from "@/hooks/useTheme";

type AppShellProps = {
    notificationInbox: ReactNode;
    setupActive?: boolean;
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
    notificationInbox,
    setupActive = false,
    mockupScreen,
    isHome,
    theme,
    onOpenAuth,
    member,
    authBusy,
    onLogout,
    children,
}: AppShellProps) {
    const [chatOpen, setChatOpen] = useState(false);
    const pageContent = useRef<HTMLDivElement>(null);
    const chatId = useId();
    const chatTrigger = useRef<HTMLButtonElement>(null);
    const chat = { id: chatId, open: chatOpen, setOpen: setChatOpen, trigger: chatTrigger };
    const navigate = useNavigate();
    const navigation = useNavigation();
    const { t, i18n } = useTranslation();
    const { pathname, search } = useLocation();
    const matchingRoute = useMatch(pagePaths.matching);
    const previewDiscovery = mockupScreen?.startsWith("matching-");
    const discoveryActive =
        previewDiscovery ||
        (matchingRoute &&
            (member || authBusy) &&
            !setupActive &&
            new URLSearchParams(search).has("searchId"));
    const inSettings = /^\/settings(?:\/|$)/i.test(pathname);
    const activeMockupScreen = previewDiscovery
        ? "matching"
        : mockupScreen === "notifications"
          ? "home"
          : mockupScreen;

    useEffect(() => {
        const content = pageContent.current;
        if (!chatOpen || !content) return;
        const mobile = window.matchMedia("(max-width: 700px)");
        const sync = () => {
            content.inert = mobile.matches;
        };
        sync();
        mobile.addEventListener("change", sync);
        return () => {
            mobile.removeEventListener("change", sync);
            content.inert = false;
        };
    }, [chatOpen]);

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
                className={`app-shell ${discoveryActive ? "app-shell--discovery" : ""} ${isHome ? "app-shell--home" : ""} ${mockupScreen === "welcome" || mockupScreen === "onboarding" ? "app-shell--intro" : ""}`}
                onClick={(event) => {
                    const target = event.target;
                    if (!chatOpen || event.defaultPrevented || !(target instanceof Element)) return;
                    if (target !== event.currentTarget && !target.closest(".app-shell__content"))
                        return;
                    if (
                        target.closest(
                            "a, button, input, textarea, select, label, form, article, dialog, [popover], [role='button'], [role='menu'], [contenteditable='true'], [tabindex='0'], .trip-map",
                        )
                    )
                        return;
                    setChatOpen(false);
                }}
            >
                {(setupActive || (!isHome && mockupScreen !== "home")) && (
                    <RoamDiscoveryLandscape subtle={!discoveryActive} />
                )}
                {navigation.state !== "idle" && (
                    <div className="route-progress" role="status">
                        <span className="visually-hidden">{t("pageState.loading.title")}</span>
                    </div>
                )}
                <header
                    className={`site-header ${inSettings || setupActive ? "site-header--settings" : ""}`}
                >
                    <Tooltip content={t("navigation.goHome")} className="brand-home-link">
                        <Link
                            className="brand-button"
                            to={mockupScreen ? mockupUrl("home") : "/"}
                            aria-label={t("navigation.goHome")}
                        >
                            <Brand />
                        </Link>
                    </Tooltip>
                    <nav className="primary-nav" aria-label={t("navigation.main")}>
                        {primaryNavigation.map((item) =>
                            item.icon === "conversation" ? (
                                <ChatSidebarButton key={item.icon} {...chat} />
                            ) : setupActive ? null : (
                                <Tooltip key={item.to} content={t(item.label)} touchHint>
                                    <NavLink
                                        aria-label={t(item.label)}
                                        to={mockupScreen ? mockupUrl(item.icon) : item.to}
                                        aria-current={
                                            mockupScreen
                                                ? activeMockupScreen === item.icon
                                                    ? "page"
                                                    : false
                                                : undefined
                                        }
                                        className={({ isActive }) =>
                                            `nav-item ${(mockupScreen ? activeMockupScreen === item.icon : isActive) ? "is-active" : ""}`
                                        }
                                    >
                                        <Icon name={item.icon} />
                                        <span>{t(item.label)}</span>
                                    </NavLink>
                                </Tooltip>
                            ),
                        )}
                    </nav>
                    <div className="header-actions">
                        {mockupScreen ? (
                            <NotificationInboxPreview
                                key={mockupScreen}
                                simulateArrival={mockupScreen === "notifications"}
                            />
                        ) : (
                            notificationInbox
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
                <ChatSidebar {...chat} />
                <div ref={pageContent} className="app-shell__content">
                    {children}
                </div>
            </div>
        </div>
    );
}
