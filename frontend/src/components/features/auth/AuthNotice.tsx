import { useTranslation } from "react-i18next";
import { Button } from "@/components/ui";
import type { AuthMessage } from "@/hooks/auth.types";

type AuthNoticeProps = {
    message: AuthMessage;
    busy: boolean;
    onLogin: () => void;
    onRetryLogout: () => void;
    onRetryProfile: () => void;
};

export function AuthNotice({
    message,
    busy,
    onLogin,
    onRetryLogout,
    onRetryProfile,
}: AuthNoticeProps) {
    const { t } = useTranslation();
    return (
        <aside className="auth-notice" aria-label={t("auth.statusLabel")}>
            <p role="alert">{t(`auth.messages.${message}`)}</p>
            <Button
                variant="outline"
                loading={busy}
                onClick={
                    message === "logoutUncertain"
                        ? onRetryLogout
                        : message === "profileUnavailable"
                          ? onRetryProfile
                          : onLogin
                }
            >
                {t(
                    message === "logoutUncertain"
                        ? "auth.retryLogout"
                        : message === "profileUnavailable"
                          ? "auth.retryProfile"
                          : "auth.login",
                )}
            </Button>
        </aside>
    );
}
