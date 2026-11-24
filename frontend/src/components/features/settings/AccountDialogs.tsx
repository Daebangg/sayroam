import { useId, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    Checkbox,
    Icon,
    Modal,
    ModalCloseButton,
    TextField,
    VerificationCode,
} from "@/components/ui";
import { ValidationPanel } from "@/components/features/auth/EmailFlowModal";
import { isValidNewPassword, newPasswordRules } from "@/hooks/authValidation";
import type { AccountFlowView, AccountPurpose, AccountResult } from "@/hooks/account.types";

type AccountActionDialogProps = AccountFlowView & {
    purpose: AccountPurpose;
    email: string | null;
    expiresIn: number;
    resendIn: number;
    onRequest: () => Promise<boolean>;
    onVerify: (code: string) => Promise<boolean>;
    onComplete: (password: string, confirmed: boolean) => Promise<boolean>;
    onClose: () => void;
};

export function AccountActionDialog({
    purpose,
    email,
    stage,
    busy,
    sending,
    message,
    expiresIn,
    resendIn,
    onRequest,
    onVerify,
    onComplete,
    onClose,
}: AccountActionDialogProps) {
    const { t } = useTranslation();
    const title = useId();
    const [code, setCode] = useState("");
    const [password, setPassword] = useState("");
    const [confirmation, setConfirmation] = useState("");
    const [agreed, setAgreed] = useState(false);
    const deletion = purpose === "ACCOUNT_DELETION";
    const visibleMessage =
        stage === "request" && ["delivery", "unavailable", "uncertain"].includes(message ?? "")
            ? null
            : message;
    const verified = stage === "verified" && (expiresIn > 0 || busy);
    const codeStage = stage === "code" && (expiresIn > 0 || busy);
    const rules = newPasswordRules(password);
    const canComplete =
        verified && (deletion ? agreed : isValidNewPassword(password) && password === confirmation);
    const blocking = busy && !sending;
    function request(): void {
        setCode("");
        setPassword("");
        setConfirmation("");
        setAgreed(false);
        void onRequest();
    }
    return (
        <Modal
            open
            onClose={onClose}
            labelledBy={title}
            className="auth-modal--flow account-action-modal"
            dismissible={!blocking}
            closeOnBackdrop={false}
        >
            <div className="auth-flow-header">
                <h2 id={title}>
                    {t(deletion ? "settings.deleteAccount" : "settings.changePassword")}
                </h2>
                <ModalCloseButton onClose={onClose} disabled={blocking} />
            </div>
            <div className="auth-flow-body" aria-busy={busy || undefined}>
                {deletion && (
                    <ul className="account-delete-notice">
                        <li>{t("accountAction.deletePersonalData")}</li>
                        <li>{t("accountAction.keepSharedRecords")}</li>
                        <li>{t("accountAction.newAccountOnRejoin")}</li>
                    </ul>
                )}
                <form
                    className="auth-flow-form"
                    onSubmit={(event) => {
                        event.preventDefault();
                        if (busy) return;
                        if (canComplete) {
                            const value = password;
                            void onComplete(value, agreed).then((success) => {
                                if (!success) {
                                    setPassword("");
                                    setConfirmation("");
                                    setCode("");
                                }
                            });
                        } else if (codeStage) {
                            void onVerify(code).then((success) => {
                                if (success) setCode("");
                            });
                        } else if (!verified) request();
                    }}
                >
                    {!verified && (
                        <div className="account-linked-email auth-stage-enter">
                            <span>{t("settings.email")}</span>
                            <strong>{email ?? t("settings.noEmail")}</strong>
                            <p>{t("accountAction.linkedEmailHint")}</p>
                        </div>
                    )}
                    {codeStage && (
                        <>
                            <VerificationCode
                                label={t("auth.codeLabel")}
                                value={code}
                                disabled={blocking}
                                onChange={setCode}
                            />
                            <div className="account-verification-meta">
                                {expiresIn > 0 && (
                                    <span>
                                        {t("accountAction.codeExpires", { seconds: expiresIn })}
                                    </span>
                                )}
                                <Button
                                    variant="plain"
                                    className="auth-text-link"
                                    disabled={busy || resendIn > 0}
                                    onClick={request}
                                >
                                    {resendIn > 0
                                        ? t("auth.resendAfter", { seconds: resendIn })
                                        : t("accountAction.resend")}
                                </Button>
                            </div>
                        </>
                    )}
                    {!verified && (
                        <>
                            {stage !== "request" && expiresIn === 0 && !busy && (
                                <p className="auth-feedback" role="alert">
                                    {t("accountAction.messages.expired")}
                                </p>
                            )}
                            {visibleMessage && (
                                <p className="auth-feedback" role="alert">
                                    {t(`accountAction.messages.${visibleMessage}`)}
                                </p>
                            )}
                        </>
                    )}
                    {verified && (
                        <>
                            <p className="auth-success auth-stage-enter" role="status">
                                <Icon name="check" variant="outline" />
                                {t("accountAction.verified", { seconds: expiresIn })}
                            </p>
                            {deletion ? (
                                <Checkbox
                                    label={t("accountAction.deleteAgreement")}
                                    checked={agreed}
                                    disabled={busy}
                                    onChange={(event) => setAgreed(event.target.checked)}
                                />
                            ) : (
                                <>
                                    <TextField
                                        label={t("accountAction.newPassword")}
                                        type="password"
                                        autoComplete="new-password"
                                        maxLength={24}
                                        required
                                        value={password}
                                        disabled={busy}
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
                                            {
                                                label: t("auth.passwordNumberRule"),
                                                valid: rules.number,
                                            },
                                            {
                                                label: t("auth.passwordSpecialRule"),
                                                valid: rules.special,
                                            },
                                            {
                                                label: t("auth.passwordWhitespaceRule"),
                                                valid: rules.noWhitespace,
                                            },
                                        ]}
                                    />
                                    <TextField
                                        label={t("auth.confirmPasswordLabel")}
                                        type="password"
                                        autoComplete="new-password"
                                        maxLength={24}
                                        required
                                        value={confirmation}
                                        disabled={busy}
                                        onChange={(event) => setConfirmation(event.target.value)}
                                    />
                                    {confirmation && password !== confirmation && (
                                        <p className="auth-feedback" role="alert">
                                            {t("accountAction.passwordMismatch")}
                                        </p>
                                    )}
                                </>
                            )}
                        </>
                    )}
                    {verified && visibleMessage && (
                        <p className="auth-feedback" role="alert">
                            {t(`accountAction.messages.${visibleMessage}`)}
                        </p>
                    )}
                    <Button
                        variant={verified && deletion ? "delete" : "confirm"}
                        type="submit"
                        loading={busy && !sending}
                        disabled={
                            busy ||
                            (verified
                                ? !canComplete
                                : codeStage
                                  ? !/^\d{6}$/.test(code)
                                  : !email || resendIn > 0)
                        }
                    >
                        {verified
                            ? t(deletion ? "settings.deleteAccount" : "settings.changePassword")
                            : codeStage
                              ? t("accountAction.verifyCode")
                              : resendIn > 0
                                ? t("auth.resendAfter", { seconds: resendIn })
                                : t("accountAction.sendCode")}
                    </Button>
                </form>
            </div>
        </Modal>
    );
}

export function AccountResultDialog({
    result,
    onClose,
    onLogin,
}: {
    result: AccountResult;
    onClose: () => void;
    onLogin: () => void;
}) {
    const { t } = useTranslation();
    const title = useId();
    const deletion = result.purpose === "ACCOUNT_DELETION";
    const key =
        result.outcome === "complete" ? (deletion ? "deleted" : "passwordChanged") : result.outcome;
    return (
        <Modal
            open
            labelledBy={title}
            onClose={onClose}
            className="ui-modal--showcase account-result-modal"
        >
            <h2 id={title}>{t(`accountAction.results.${key}.title`)}</h2>
            <p role={result.outcome === "complete" ? "status" : "alert"}>
                {t(`accountAction.results.${key}.message`)}
            </p>
            <div className="account-dialog-actions">
                <Button variant="close" onClick={onClose}>
                    {t("common.close")}
                </Button>
                {(!deletion || result.outcome !== "complete") && (
                    <Button variant="brand" onClick={onLogin}>
                        {t("auth.login")}
                    </Button>
                )}
            </div>
        </Modal>
    );
}
