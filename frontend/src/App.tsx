import { socialReturn } from "@/routing/socialReturn";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, ScrollRestoration, useLocation, useNavigate, useNavigation } from "react-router";
import { AppShell } from "@/components/layout/AppShell";
import { AuthDialog } from "@/pages/auth/AuthDialog";
import { MemberGate } from "@/components/features/auth/MemberGate";
import { AuthNotice } from "@/components/features/auth/AuthNotice";
import { useAuthSession } from "@/hooks/api/auth/useAuthSession";
import type { LoginInput } from "@/hooks/auth.types";
import { isMemberPath, pagePaths } from "@/routing/paths";
import { getMockupScreen } from "@/routing/mockups";
import { setLanguage } from "@/i18n";
import type { Language } from "@/i18n/locale";
import { useTheme, type ThemePreference } from "@/hooks/useTheme";
import { LoadingPage } from "@/pages/status/StatusPage";
import { RouteAccessibility } from "@/routing/RouteAccessibility";
import { Toast } from "@/components/ui";

type AuthToast =
    | "socialSignupComplete"
    | "signupComplete"
    | "resetComplete"
    | "loginComplete"
    | "logoutComplete";

export default function App() {
    const { i18n, t } = useTranslation();
    const [toast, setToast] = useState<{ id: number; message: AuthToast } | null>(null);
    const toastSequence = useRef(0);

    function showToast(message: AuthToast): void {
        setToast({ id: ++toastSequence.current, message });
    }
    const location = useLocation();
    const mockupScreen = getMockupScreen(location.pathname, location.search);
    const navigate = useNavigate();
    const navigation = useNavigation();
    const isPageLoading =
        navigation.state !== "idle" && navigation.location?.pathname !== location.pathname;
    const theme = useTheme();
    const auth = useAuthSession(socialReturn !== null);
    const checkSession = auth.check;
    const authBusy = ["checking", "signingIn", "signingOut"].includes(auth.status);
    const modalOnlyAuthMessage =
        auth.message?.startsWith("social") ||
        auth.message === "credentials" ||
        auth.message === "limited" ||
        auth.message === "invalidInput" ||
        auth.message === "loginUncertain";
    useEffect(() => {
        if (isMemberPath(location.pathname)) void checkSession();
    }, [location.pathname, checkSession]);
    const [authOpen, setAuthOpen] = useState(
        () =>
            socialReturn !== null ||
            new URLSearchParams(window.location.search).get("modal") === "auth",
    );
    const socialReturnHandled = useRef(false);
    const exchangeSocial = auth.exchangeSocial;
    useEffect(() => {
        const returned = socialReturn;
        if (!returned || socialReturnHandled.current) return;
        void exchangeSocial(returned.code).then((success) => {
            socialReturnHandled.current = true;
            if (!success) return;
            setAuthOpen(false);
            setToast({ id: ++toastSequence.current, message: "loginComplete" });
            void navigate(returned.returnPath, { replace: true });
        });
    }, [exchangeSocial, navigate]);
    const authTrigger = useRef<HTMLElement | null>(null);
    useLayoutEffect(() => {
        if (authOpen || !authTrigger.current) return;
        const target = authTrigger.current.isConnected
            ? authTrigger.current
            : document.querySelector<HTMLElement>("main h1");
        target?.focus({ preventScroll: true });
        authTrigger.current = null;
    }, [authOpen]);

    function openAuth(): void {
        if (auth.user) {
            void navigate(pagePaths.trips);
            return;
        }
        authTrigger.current =
            document.activeElement instanceof HTMLElement ? document.activeElement : null;
        setAuthOpen(true);
    }

    function closeAuth(): void {
        setAuthOpen(false);
        const current = new URL(window.location.href);
        const params = current.searchParams;
        if (params.get("modal") === "auth") {
            params.delete("modal");
            const search = params.toString();
            void navigate(current.pathname + (search ? `?${search}` : "") + current.hash, {
                replace: true,
                preventScrollReset: true,
            });
        }
    }

    async function login(input: LoginInput): Promise<boolean> {
        setToast(null);
        const success = await auth.login(input);
        if (success) {
            closeAuth();
            showToast("loginComplete");
        }
        return success;
    }

    function logout(): void {
        setToast(null);
        closeAuth();
        void auth.logout().then((success) => {
            if (success) showToast("logoutComplete");
        });
        if (isMemberPath(location.pathname)) void navigate(pagePaths.plaza, { replace: true });
    }

    function retryProfile(): void {
        void auth.restore().then((success) => {
            if (success) {
                closeAuth();
                showToast("loginComplete");
                if (socialReturn) void navigate(socialReturn.returnPath, { replace: true });
            }
        });
    }

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
            mockupScreen={mockupScreen}
            isHome={location.pathname === "/"}
            theme={theme.resolved}
            themePreference={theme.preference}
            onThemeChange={changeTheme}
            onLanguageChange={changeLanguage}
            onOpenAuth={openAuth}
            member={auth.user}
            authBusy={authBusy}
            onLogout={logout}
        >
            {auth.message && !authOpen && !modalOnlyAuthMessage && (
                <AuthNotice
                    message={auth.message}
                    busy={authBusy}
                    onLogin={openAuth}
                    onRetryLogout={logout}
                    onRetryProfile={retryProfile}
                />
            )}
            {isPageLoading ? (
                <LoadingPage />
            ) : isMemberPath(location.pathname) && !auth.user ? (
                <MemberGate
                    checking={authBusy}
                    unavailable={auth.status === "unavailable"}
                    onLogin={openAuth}
                />
            ) : (
                <Outlet
                    context={{
                        onOpenAuth: openAuth,
                        onLanguageChange: () =>
                            changeLanguage(i18n.resolvedLanguage === "ko" ? "en" : "ko"),
                    }}
                />
            )}
            <RouteAccessibility
                contentKey={
                    mockupScreen ?? (isMemberPath(location.pathname) ? auth.status : undefined)
                }
            />
            <ScrollRestoration />
            {authOpen && (
                <AuthDialog
                    {...(auth.socialSignup ? { socialSignup: auth.socialSignup } : {})}
                    onSocialComplete={async (input) => {
                        const success = await auth.completeSocial(input);
                        if (success) {
                            closeAuth();
                            showToast("socialSignupComplete");
                            if (socialReturn)
                                void navigate(socialReturn.returnPath, { replace: true });
                        }
                        return success;
                    }}
                    onCancelSocial={auth.cancelSocial}
                    onClose={closeAuth}
                    onSubmit={login}
                    busy={authBusy}
                    message={auth.message}
                    {...(auth.retryAt === undefined ? {} : { retryAt: auth.retryAt })}
                    {...(auth.remainingAttempts === undefined
                        ? {}
                        : { remainingAttempts: auth.remainingAttempts })}
                    onRetryProfile={retryProfile}
                    onComplete={(mode) =>
                        showToast(mode === "signup" ? "signupComplete" : "resetComplete")
                    }
                />
            )}
            {toast && (
                <Toast
                    key={toast.id}
                    message={t(`auth.${toast.message}`)}
                    onDismiss={() => setToast(null)}
                />
            )}
        </AppShell>
    );
}
