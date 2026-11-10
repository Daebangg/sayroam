import { useInputValidation } from "@/hooks/useInputValidation";
import { useServerErrors } from "@/hooks/useServerErrors";
import { StatusPage } from "@/pages/status/StatusPage";
import { socialReturn } from "@/routing/socialReturn";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, ScrollRestoration, useLocation, useNavigate } from "react-router";
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
import { RouteAccessibility } from "@/routing/RouteAccessibility";
import { Toast } from "@/components/ui";
import { AccountResultDialog } from "@/components/features/settings/AccountDialogs";
import type { AccountResult, ProfileDetailsUpdate } from "@/hooks/account.types";

type AuthToast =
    | "signupComplete"
    | "socialSignupComplete"
    | "resetComplete"
    | "loginComplete"
    | "logoutComplete"
    | "nicknameSaved"
    | "profileImageSaved"
    | "profileDetailsSaved"
    | "badGateway"
    | "passwordChanged"
    | "accountDeleted";

export default function App() {
    const { i18n, t } = useTranslation();
    const [toast, setToast] = useState<
        | ({ id: number; error?: boolean } & (
              { message: AuthToast; text?: never } | { text: string; message?: never }
          ))
        | null
    >(null);
    const [recovering, setRecovering] = useState(false);
    const [accountResult, setAccountResult] = useState<AccountResult | null>(null);
    const toastSequence = useRef(0);
    useServerErrors(() =>
        setToast({ id: ++toastSequence.current, message: "badGateway", error: true }),
    );

    useInputValidation((message, field) => {
        const last = field.charCodeAt(field.length - 1);
        const particle =
            last >= 0xac00 && last <= 0xd7a3 && (last - 0xac00) % 28 !== 0 ? "을" : "를";
        const text =
            message === "required" && field
                ? t("validation.requiredField", {
                      field: i18n.resolvedLanguage === "ko" ? field + particle : field,
                  })
                : t(`validation.${message}`);
        setToast({ id: ++toastSequence.current, text, error: true });
    });

    function showToast(message: AuthToast): void {
        setToast({ id: ++toastSequence.current, message });
    }
    const location = useLocation();
    const mockupScreen = getMockupScreen(location.pathname, location.search);
    const navigate = useNavigate();
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
            socialReturn === null &&
            new URLSearchParams(window.location.search).get("modal") === "auth",
    );
    const socialReturnHandled = useRef(false);
    const exchangeSocial = auth.exchangeSocial;
    useEffect(() => {
        const returned = socialReturn;
        if (!returned || socialReturnHandled.current) return;
        let current = true;
        void exchangeSocial(returned.code).then((success) => {
            if (!current) return;
            socialReturnHandled.current = true;
            setAuthOpen(!success);
            if (!success) return;
            setToast({
                id: ++toastSequence.current,
                message: success === "signup" ? "socialSignupComplete" : "loginComplete",
            });
            void navigate(returned.returnPath, { replace: true });
        });
        return () => {
            current = false;
        };
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
        if (socialReturn && !socialReturnHandled.current) return;
        if (auth.user) {
            void navigate(pagePaths.trips);
            return;
        }
        authTrigger.current =
            document.activeElement instanceof HTMLElement ? document.activeElement : null;
        setAuthOpen(true);
    }

    function closeAuth(): void {
        auth.resetDialog();
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
        setAuthOpen(false);
        setRecovering(true);
        void auth
            .restore()
            .then((success) => {
                if (success) {
                    closeAuth();
                    showToast("loginComplete");
                    if (socialReturn) void navigate(socialReturn.returnPath, { replace: true });
                }
            })
            .finally(() => setRecovering(false));
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

    const normalizedPath = location.pathname.replace(/\/+$/, "").toLowerCase() || "/";
    const knownPage = Object.values(pagePaths).some((path) => path === normalizedPath);
    const showAuthFailure =
        knownPage && auth.status === "unavailable" && auth.message !== "logoutUncertain";
    return (
        <AppShell
            mockupScreen={mockupScreen}
            isHome={location.pathname === "/"}
            theme={theme.resolved}
            onOpenAuth={openAuth}
            member={auth.user}
            authBusy={authBusy}
            onLogout={logout}
        >
            {auth.message && !authOpen && !modalOnlyAuthMessage && !showAuthFailure && (
                <AuthNotice
                    message={auth.message}
                    busy={authBusy}
                    onLogin={openAuth}
                    onRetryLogout={logout}
                    onRetryProfile={retryProfile}
                />
            )}
            {showAuthFailure ? (
                <StatusPage
                    kind={(auth.failureStatus ?? 0) >= 500 ? "serverError" : "error"}
                    onRetry={retryProfile}
                    reloadHome
                />
            ) : recovering && authBusy ? (
                <StatusPage kind="loading" />
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
                        member: auth.user,
                        themePreference: theme.preference,
                        onThemeChange: changeTheme,
                        onSelectLanguage: changeLanguage,
                        onUpdateProfileDetails: async (
                            input: ProfileDetailsUpdate,
                            signal: AbortSignal,
                        ) => {
                            await auth.updateProfileDetails(input, signal);
                            showToast("profileDetailsSaved");
                        },
                        onUploadProfileImage: async (dataUrl: string, signal: AbortSignal) => {
                            await auth.uploadProfileImage(dataUrl, signal);
                            showToast("profileImageSaved");
                        },
                        onUpdateProfileImage: async (
                            profileImageUrl: string,
                            signal: AbortSignal,
                        ) => {
                            await auth.updateProfileImage(profileImageUrl, signal);
                            showToast("profileImageSaved");
                        },
                        onUpdateNickname: async (nickname: string, signal: AbortSignal) => {
                            await auth.updateNickname(nickname, signal);
                            showToast("nicknameSaved");
                        },
                        onAccountResult: (result: AccountResult) => {
                            auth.endSession();
                            setAccountResult(result.outcome === "complete" ? null : result);
                            if (result.outcome === "complete")
                                showToast(
                                    result.purpose === "ACCOUNT_DELETION"
                                        ? "accountDeleted"
                                        : "passwordChanged",
                                );
                            void navigate(pagePaths.plaza, { replace: true });
                        },
                    }}
                />
            )}
            <RouteAccessibility
                contentKey={
                    mockupScreen ??
                    (isMemberPath(location.pathname) || showAuthFailure || recovering
                        ? auth.status
                        : undefined)
                }
            />
            <ScrollRestoration />
            {authOpen && !showAuthFailure && (
                <AuthDialog
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
                    message={toast.text ?? t(`auth.${toast.message}`)}
                    tone={toast.error ? "error" : "success"}
                    durationMs={toast.error ? 6000 : 3000}
                    onDismiss={() => setToast(null)}
                />
            )}
            {accountResult && (
                <AccountResultDialog
                    result={accountResult}
                    onClose={() => setAccountResult(null)}
                    onLogin={() => {
                        setAccountResult(null);
                        openAuth();
                    }}
                />
            )}
        </AppShell>
    );
}
