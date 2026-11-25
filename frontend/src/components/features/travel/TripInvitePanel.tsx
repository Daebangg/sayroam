import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";

type TripInvitePanelProps = {
    link: string | null;
    loading: boolean;
    failed: boolean;
    onRetry: () => void;
    onCopy: () => void;
    onClose: () => void;
};

// 패널을 열면 페이지가 지금 쓸 수 있는 링크를 받아 온다. 링크를 따로 만드는 단계는 없다.
export function TripInvitePanel({
    link,
    loading,
    failed,
    onRetry,
    onCopy,
    onClose,
}: TripInvitePanelProps) {
    const { t } = useTranslation();
    return (
        <section
            className="trip-side-panel trip-invite"
            aria-labelledby="trip-invite-title"
            onKeyDown={(event) => {
                if (event.key === "Escape") onClose();
            }}
        >
            <header className="trip-side-panel__header">
                <h2 id="trip-invite-title">{t("tripDetail.invite")}</h2>
                <Button
                    variant="plain"
                    className="trip-side-panel__close"
                    aria-label={t("tripDetail.inviteClose")}
                    onClick={onClose}
                >
                    <Icon name="close" variant="outline" />
                </Button>
            </header>
            <p className="trip-invite__text">{t("tripDetail.inviteDescription")}</p>
            {loading && (
                <p className="trip-invite__text" role="status">
                    {t("tripDetail.inviteLoading")}
                </p>
            )}
            {!loading && link && (
                <div className="trip-invite__link">
                    <span className="trip-invite__label" id="trip-invite-link">
                        {t("tripDetail.inviteLink")}
                    </span>
                    <div className="trip-invite__row">
                        <output aria-labelledby="trip-invite-link">{link}</output>
                        <Button onClick={onCopy}>{t("tripDetail.inviteCopy")}</Button>
                    </div>
                    <span className="trip-invite__text">{t("tripDetail.inviteExpires")}</span>
                </div>
            )}
            {!loading && failed && (
                <>
                    <p className="trip-invite__error" role="alert">
                        {t("tripDetail.inviteFailed")}
                    </p>
                    <Button variant="outline" onClick={onRetry}>
                        {t("tripDetail.retry")}
                    </Button>
                </>
            )}
        </section>
    );
}
