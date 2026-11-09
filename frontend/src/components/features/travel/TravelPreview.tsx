import { useTranslation } from "react-i18next";
import { Badge, Button, Icon, NotificationBadge, TravelArtwork } from "@/components/ui";
import { formatDateRange, formatNumber } from "@/i18n/locale";
import type { PreviewRoom, PreviewTrip } from "@/data/preview";

type TripProps = { trip: PreviewTrip; onOpen: () => void };

export function TravelMeta({ trip }: { trip: PreviewTrip }) {
    const { t, i18n } = useTranslation();
    const language = i18n.resolvedLanguage ?? "ko";
    return (
        <span className="travel-meta">
            <span>
                <Icon name="calendar" variant="outline" />
                {formatDateRange(language, trip.startDate, trip.endDate)}
            </span>
            <span>
                <Icon name="matching" variant="outline" />
                {t("matching.people", {
                    current: formatNumber(language, trip.participants),
                    total: formatNumber(language, trip.capacity),
                })}
            </span>
        </span>
    );
}

export function FeaturedTrip({ trip, onOpen }: TripProps) {
    const { t } = useTranslation();
    return (
        <Button variant="plain" className="featured-trip" onClick={onOpen}>
            <TravelArtwork scene={trip.scene} />
            <span className="featured-trip__badge">
                <Badge tone="warm" icon="flame" label={t("mockup.popular")} />
            </span>
            <span className="featured-trip__caption">
                <strong>{t(trip.title)}</strong>
                <TravelMeta trip={trip} />
            </span>
        </Button>
    );
}

export function TravelRow({
    trip,
    onOpen,
    saved,
    onSave,
}: TripProps & { saved: boolean; onSave: () => void }) {
    const { t } = useTranslation();
    return (
        <article className="travel-row">
            <Button variant="plain" className="travel-row__main" onClick={onOpen}>
                <TravelArtwork scene={trip.scene} />
                <span className="travel-row__body">
                    <strong>{t(trip.title)}</strong>
                    <TravelMeta trip={trip} />
                </span>
            </Button>
            <Button
                variant="ghost"
                className="icon-button bookmark-button"
                aria-label={t(saved ? "mockup.unsave" : "mockup.saveTrip", {
                    title: t(trip.title),
                })}
                aria-pressed={saved}
                onClick={onSave}
            >
                <Icon name="bookmark" variant="outline" />
            </Button>
        </article>
    );
}

export function RoomPreviewRow({ room, onOpen }: { room: PreviewRoom; onOpen: () => void }) {
    const { t } = useTranslation();
    return (
        <Button variant="plain" className="room-preview-row" onClick={onOpen}>
            <TravelArtwork scene={room.scene} />
            <span className="room-preview-row__body">
                <strong>{t(room.title)}</strong>
                <span>{t(room.message)}</span>
            </span>
            <span className="room-preview-row__aside">
                <time>{t(room.time)}</time>
                <NotificationBadge
                    count={room.unread}
                    label={t("mockup.unread", { count: room.unread })}
                />
            </span>
        </Button>
    );
}
