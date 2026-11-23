import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";

type TripInvitePanelProps = {
    /** 이미 채팅방이 연결된 여행인지. 아니면 처음 초대할 때 채팅방이 만들어진다. */
    hasRoom: boolean;
    link: string | null;
    creating: boolean;
    failed: boolean;
    onCreate: () => void;
    onCopy: () => void;
    onClose: () => void;
};

export function TripInvitePanel({
    hasRoom,
    link,
    creating,
    failed,
    onCreate,
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
            {!hasRoom && <p className="trip-invite__text">{t("tripDetail.inviteNeedsRoom")}</p>}
            {link && (
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
            {failed && (
                <p className="trip-invite__error" role="alert">
                    {t("tripDetail.inviteFailed")}
                </p>
            )}
            <Button variant={link ? "outline" : "primary"} loading={creating} onClick={onCreate}>
                {t(link ? "tripDetail.inviteRenew" : "tripDetail.inviteCreate")}
            </Button>
        </section>
    );
}
