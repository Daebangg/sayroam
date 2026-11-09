import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Checkbox, Icon, Modal, TextField, Tooltip } from "@/components/ui";
import type { AuthMessage, LoginInput } from "@/hooks/auth.types";
import { isValidEmail, isValidPassword } from "@/hooks/authValidation";

type LoginModalProps = {
    open: boolean;
    onClose: () => void;
    onSubmit: (input: LoginInput) => Promise<boolean>;
    busy: boolean;
    message: AuthMessage | null;
    onRetryProfile: () => void;
    onMode: (mode: "signup" | "reset") => void;
};

export function LoginModal({
    open,
    onClose,
    onSubmit,
    busy,
    message,
    onRetryProfile,
    onMode,
}: LoginModalProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const errorId = useId();
    const errorRef = useRef<HTMLParagraphElement>(null);
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [emailReady, setEmailReady] = useState(false);
    const [localError, setLocalError] = useState<"invalidEmail" | "invalidPassword" | null>(null);
    useEffect(() => {
        if (message || localError) errorRef.current?.focus();
    }, [message, localError]);

    return (
        <Modal open={open} onClose={onClose} labelledBy={titleId} className="auth-modal">
            <Tooltip content={t("common.close")} shortcut={["Escape"]} className="ui-modal__close">
                <Button
                    variant="ghost"
                    className="icon-button"
                    onClick={onClose}
                    aria-label={t("common.close")}
                >
                    <Icon name="close" variant="outline" />
                </Button>
            </Tooltip>
            <h2 id={titleId}>{t("auth.title")}</h2>
            <p className="auth-preview__description">{t("auth.description")}</p>
            <div className="auth-preview__options">
                <Button
                    variant="outline"
                    className="oauth-button oauth-button--google"
                    disabled
                    title={t("common.disabled")}
                >
                    <img className="oauth-logo" src="/brand/providers/google.png" alt="" />
                    {t("auth.google")}
                </Button>
                <Button
                    variant="soft"
                    className="oauth-button oauth-button--kakao"
                    disabled
                    title={t("common.disabled")}
                >
                    <img className="oauth-logo" src="/brand/providers/kakao.svg" alt="" />
                    {t("auth.kakao")}
                </Button>
                <Button
                    variant="soft"
                    className="oauth-button oauth-button--naver"
                    disabled
                    title={t("common.disabled")}
                >
                    <span className="oauth-logo oauth-logo--naver">
                        <img src="/brand/providers/naver.png" alt="" />
                    </span>
                    {t("auth.naver")}
                </Button>
            </div>
            <div className="auth-preview__divider">
                <span>{t("auth.or")}</span>
            </div>
            <form
                className="auth-preview__email"
                noValidate
                aria-describedby={message || localError ? errorId : undefined}
                onSubmit={(event) => {
                    event.preventDefault();
                    if (busy) return;
                    if (!isValidEmail(email)) {
                        setLocalError("invalidEmail");
                        return;
                    }
                    if (!emailReady) {
                        setEmailReady(true);
                        setLocalError(null);
                        return;
                    }
                    if (!isValidPassword(password)) {
                        setLocalError("invalidPassword");
                        return;
                    }
                    setLocalError(null);
                    const submittedPassword = password;
                    setPassword("");
                    void onSubmit({ email, password: submittedPassword });
                }}
            >
                <TextField
                    label={t("auth.emailLabel")}
                    name="email"
                    type="email"
                    autoComplete="username"
                    required
                    maxLength={254}
                    value={email}
                    disabled={busy}
                    onChange={(event) => {
                        setEmail(event.target.value);
                        setEmailReady(false);
                        setPassword("");
                        setLocalError(null);
                    }}
                />
                {emailReady && (
                    <TextField
                        label={t("auth.passwordLabel")}
                        name="password"
                        type="password"
                        autoComplete="current-password"
                        required
                        minLength={8}
                        maxLength={128}
                        value={password}
                        disabled={busy}
                        autoFocus
                        onChange={(event) => {
                            setPassword(event.target.value);
                            setLocalError(null);
                        }}
                    />
                )}
                {(message || localError) && (
                    <p
                        id={errorId}
                        ref={errorRef}
                        tabIndex={-1}
                        role="alert"
                        className="auth-feedback"
                    >
                        {localError
                            ? localError === "invalidPassword"
                                ? t("auth.loginPasswordRule")
                                : t("auth.flowMessages.invalidEmail")
                            : message
                              ? t(`auth.messages.${message}`)
                              : null}
                    </p>
                )}
                {emailReady && <Checkbox label={t("auth.rememberSession")} checked disabled />}
                {message === "profileUnavailable" ? (
                    <Button variant="brand" onClick={onRetryProfile} loading={busy}>
                        {t("auth.retryProfile")}
                    </Button>
                ) : (
                    <Button variant="brand" type="submit" loading={busy}>
                        {t(busy ? "auth.signingIn" : emailReady ? "auth.login" : "auth.startEmail")}
                    </Button>
                )}
            </form>
            <div className="auth-preview__links">
                <p>
                    {t("auth.newHere")}{" "}
                    <Button
                        variant="plain"
                        className="auth-text-link"
                        type="button"
                        disabled={busy}
                        onClick={() => onMode("signup")}
                    >
                        {t("auth.signup")}
                    </Button>
                </p>
                <p>
                    {t("auth.forgot")}{" "}
                    <Button
                        variant="plain"
                        className="auth-text-link"
                        type="button"
                        disabled={busy}
                        onClick={() => onMode("reset")}
                    >
                        {t("auth.reset")}
                    </Button>
                </p>
            </div>
        </Modal>
    );
}
