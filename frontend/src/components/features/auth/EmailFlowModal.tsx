import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, Modal, TextField, VerificationCode, Tooltip } from "@/components/ui";
import {
    isValidEmail,
    isValidNewPassword,
    isValidNickname,
    newPasswordRules,
} from "@/hooks/authValidation";
import type { EmailFlowMessage } from "@/hooks/auth.types";

type EmailFlowModalProps = {
    mode: "signup" | "reset";
    busy: boolean;
    sending: boolean;
    sent: boolean;
    verified: boolean;
    expiresIn: number;
    resendIn: number;
    message: EmailFlowMessage | null;
    onEmailChange: () => void;
    onRequest: (email: string) => Promise<boolean>;
    onVerify: (code: string) => Promise<boolean>;
    onComplete: (input: { nickname: string; password: string }) => Promise<boolean>;
    onLogin: () => void;
    onClose: () => void;
};

export function ValidationPanel({
    title,
    items,
}: {
    title: string;
    items: { label: string; valid: boolean }[];
}) {
    return (
        <div className="auth-validation">
            <strong>{title}</strong>
            <ul>
                {items.map((item) => (
                    <li key={item.label} className={item.valid ? "is-valid" : ""}>
                        <span className="auth-validation__mark" aria-hidden="true">
                            {item.valid ? "✓" : "!"}
                        </span>
                        <span>{item.label}</span>
                    </li>
                ))}
            </ul>
        </div>
    );
}

export function EmailFlowModal({
    mode,
    busy,
    sending,
    sent,
    verified,
    expiresIn,
    resendIn,
    message,
    onEmailChange,
    onRequest,
    onVerify,
    onComplete,
    onLogin,
    onClose,
}: EmailFlowModalProps) {
    const { t } = useTranslation();
    const titleId = useId();
    const errorId = useId();
    const errorRef = useRef<HTMLParagraphElement>(null);
    const [email, setEmail] = useState("");
    const [nickname, setNickname] = useState("");
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [code, setCode] = useState("");
    const blocking = busy && !sending;
    const validPassword = isValidNewPassword(password);
    const rules = newPasswordRules(password);
    const matches = password.length > 0 && password === confirmation;
    const canComplete =
        verified && validPassword && matches && (mode === "reset" || isValidNickname(nickname));
    useEffect(() => {
        if (message) errorRef.current?.focus();
    }, [message]);

    return (
        <Modal
            open
            onClose={onClose}
            labelledBy={titleId}
            className="auth-modal--flow"
            closeOnBackdrop={false}
        >
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
            <div className="auth-flow-header">
                <h2 id={titleId}>{t(mode === "signup" ? "auth.signupTitle" : "auth.reset")}</h2>
                <p className="auth-preview__description">
                    {t(mode === "signup" ? "auth.signupDescription" : "auth.resetDescription")}
                </p>
            </div>
            <div className="auth-flow-body">
                <form
                    className="auth-flow-form"
                    onSubmit={(event) => {
                        event.preventDefault();
                        if (busy) return;
                        if (mode === "reset" && !verified) {
                            void onRequest(email);
                            return;
                        }
                        if (!canComplete) return;
                        const input = { nickname, password };
                        setPassword("");
                        setConfirmation("");
                        setCode("");
                        void onComplete(input);
                    }}
                    aria-describedby={message ? errorId : undefined}
                >
                    {mode !== "reset" && (
                        <TextField
                            label={t("auth.nicknameLabel")}
                            name="nickname"
                            autoComplete="nickname"
                            maxLength={50}
                            required
                            value={nickname}
                            disabled={blocking}
                            onChange={(event) => setNickname(event.target.value)}
                        />
                    )}
                    <TextField
                        label={t("auth.emailLabel")}
                        name="email"
                        type="email"
                        autoComplete="email"
                        required
                        maxLength={254}
                        value={email}
                        disabled={blocking}
                        readOnly={sending}
                        onChange={(event) => {
                            setEmail(event.target.value);
                            setCode("");
                            onEmailChange();
                        }}
                    />
                    {mode !== "reset" && (
                        <ValidationPanel
                            title={t("auth.emailValidation")}
                            items={[{ label: t("auth.emailRule"), valid: isValidEmail(email) }]}
                        />
                    )}
                    {(mode !== "reset" || sent || sending) && (
                        <section className="auth-verification" aria-label={t("auth.verifyEmail")}>
                            {verified ? (
                                <p className="auth-success" role="status">
                                    {t("auth.verified", { seconds: expiresIn })}
                                </p>
                            ) : (
                                <>
                                    {(sent || sending) && (
                                        <>
                                            {sent && (
                                                <p className="auth-flow-hint" role="status">
                                                    {t(
                                                        mode === "reset"
                                                            ? "auth.resetCodeSent"
                                                            : "auth.codeSent",
                                                        { seconds: expiresIn },
                                                    )}
                                                </p>
                                            )}
                                            <VerificationCode
                                                label={t("auth.codeLabel")}
                                                value={code}
                                                disabled={blocking}
                                                onChange={setCode}
                                            />
                                            <Button
                                                variant="brand"
                                                disabled={
                                                    !/^\d{6}$/.test(code) || expiresIn === 0 || busy
                                                }
                                                onClick={() => {
                                                    void onVerify(code).then((success) => {
                                                        if (success) setCode("");
                                                    });
                                                }}
                                            >
                                                {t("auth.verifyCode")}
                                            </Button>
                                            {!sending && expiresIn === 0 && (
                                                <p className="auth-feedback">
                                                    {t("auth.flowMessages.expired")}
                                                </p>
                                            )}
                                        </>
                                    )}
                                    <Button
                                        variant="brand"
                                        disabled={!isValidEmail(email) || resendIn > 0 || busy}
                                        onClick={() => {
                                            setCode("");
                                            void onRequest(email);
                                        }}
                                    >
                                        {resendIn > 0
                                            ? t("auth.resendAfter", { seconds: resendIn })
                                            : t(
                                                  sent || sending
                                                      ? "auth.resendCode"
                                                      : "auth.sendCode",
                                              )}
                                    </Button>
                                </>
                            )}
                        </section>
                    )}
                    {(mode === "signup" || verified) && (
                        <>
                            <TextField
                                label={t(
                                    mode === "reset"
                                        ? "auth.newPasswordLabel"
                                        : "auth.passwordLabel",
                                )}
                                name="password"
                                type="password"
                                autoComplete="new-password"
                                required
                                minLength={8}
                                maxLength={24}
                                value={password}
                                disabled={blocking}
                                onChange={(event) => setPassword(event.target.value)}
                            />
                            <ValidationPanel
                                title={t("auth.passwordValidation")}
                                items={[
                                    {
                                        label: t("auth.passwordLengthRule"),
                                        valid: rules.length,
                                    },
                                    {
                                        label: t("auth.passwordUppercaseRule"),
                                        valid: rules.uppercase,
                                    },
                                    { label: t("auth.passwordNumberRule"), valid: rules.number },
                                    { label: t("auth.passwordSpecialRule"), valid: rules.special },
                                    {
                                        label: t("auth.passwordWhitespaceRule"),
                                        valid: rules.noWhitespace,
                                    },
                                ]}
                            />
                            <TextField
                                label={t("auth.confirmPasswordLabel")}
                                name="confirmation"
                                type="password"
                                autoComplete="new-password"
                                required
                                maxLength={24}
                                value={confirmation}
                                disabled={blocking}
                                onChange={(event) => setConfirmation(event.target.value)}
                            />
                            <ValidationPanel
                                title={t("auth.confirmPasswordLabel")}
                                items={[{ label: t("auth.passwordMatchRule"), valid: matches }]}
                            />
                        </>
                    )}
                    {message && (
                        <p
                            id={errorId}
                            ref={errorRef}
                            role="alert"
                            tabIndex={-1}
                            className="auth-feedback"
                        >
                            {t(`auth.flowMessages.${message}`)}
                        </p>
                    )}
                    {(mode === "signup" || verified || (!sent && !sending)) && (
                        <Button
                            variant="brand"
                            type="submit"
                            loading={mode === "reset" ? busy : blocking}
                            disabled={
                                mode === "reset" && !verified
                                    ? sent || !isValidEmail(email) || resendIn > 0 || busy
                                    : !canComplete || busy
                            }
                        >
                            {mode === "reset" && !verified && resendIn > 0
                                ? t("auth.resendAfter", { seconds: resendIn })
                                : t(
                                      mode === "signup"
                                          ? "auth.createAccount"
                                          : verified
                                            ? "auth.setPassword"
                                            : "auth.sendCode",
                                  )}
                        </Button>
                    )}
                </form>
                <p className="auth-flow-footer">
                    {t(mode !== "reset" ? "auth.haveAccount" : "auth.rememberPassword")}{" "}
                    <Button
                        variant="plain"
                        className="auth-text-link"
                        onClick={onLogin}
                        disabled={blocking}
                    >
                        {t("auth.backToLogin")}
                    </Button>
                </p>
            </div>
        </Modal>
    );
}
