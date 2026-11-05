import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Checkbox, Icon, Modal, TextField, Tooltip } from "@/components/ui";

type AuthPreviewProps = {
    open: boolean;
    onClose: () => void;
};

export function AuthPreview({ open, onClose }: AuthPreviewProps) {
    const { t } = useTranslation();
    const titleId = useId();

    return (
        <Modal open={open} onClose={onClose} labelledBy={titleId}>
            <Tooltip content={t("common.close")} shortcut={["Escape"]} className="ui-modal__close">
                <Button
                    variant="ghost"
                    className="icon-button"
                    onClick={onClose}
                    aria-label={t("common.close")}
                >
                    <Icon name="close" />
                </Button>
            </Tooltip>
            <h2 id={titleId}>{t("auth.title")}</h2>
            <p className="auth-preview__description">{t("auth.description")}</p>
            <div className="auth-preview__options">
                <Button variant="outline" className="oauth-button oauth-button--google">
                    <img className="oauth-logo" src="/brand/providers/google.png" alt="" />
                    {t("auth.google")}
                </Button>
                <Button variant="soft" className="oauth-button oauth-button--kakao">
                    <img className="oauth-logo" src="/brand/providers/kakao.svg" alt="" />
                    {t("auth.kakao")}
                </Button>
                <Button variant="soft" className="oauth-button oauth-button--naver">
                    <span className="oauth-logo oauth-logo--naver">
                        <img src="/brand/providers/naver.png" alt="" />
                    </span>
                    {t("auth.naver")}
                </Button>
            </div>
            <div className="auth-preview__divider">
                <span>{t("auth.or")}</span>
            </div>
            <form className="auth-preview__email" onSubmit={(event) => event.preventDefault()}>
                <Checkbox label={t("auth.remember")} />
                <TextField label={t("auth.emailLabel")} type="email" autoComplete="email" />
                <Button variant="brand" type="submit">
                    {t("auth.startEmail")}
                </Button>
            </form>
            <div className="auth-preview__links">
                <p>
                    {t("auth.newHere")} <button type="button">{t("auth.signup")}</button>
                </p>
                <p>
                    {t("auth.forgot")} <button type="button">{t("auth.reset")}</button>
                </p>
            </div>
        </Modal>
    );
}
