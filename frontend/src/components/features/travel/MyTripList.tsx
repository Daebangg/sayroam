import { useTranslation } from "react-i18next";
import { Badge, Card, Icon, TravelArtwork } from "@/components/ui";
import { formatDateRange } from "@/i18n/locale";
import type { MyTrip } from "@/hooks/trip.types";

type MyTripListProps = { trips: MyTrip[] };

const scenes = ["mountain", "coast", "city", "island"] as const;

function sceneOf(trip: MyTrip): (typeof scenes)[number] {
    return scenes[trip.tripId % scenes.length] ?? "mountain";
}

function TripMeta({ trip }: { trip: MyTrip }) {
    const { t, i18n } = useTranslation();
    return (
        <span className="travel-meta">
            <span>
                <Icon name="calendar" variant="outline" />
                {formatDateRange(i18n.resolvedLanguage ?? "ko", trip.startDate, trip.endDate)}
            </span>
            <span>
                <Icon name="matching" variant="outline" />
                {t(trip.roomId === null ? "trips.solo" : "trips.withRoom")}
            </span>
        </span>
    );
}

export function MyTripList({ trips }: MyTripListProps) {
    const { t } = useTranslation();
    return (
        <>
            <ul
                className="trip-grid trip-grid--desktop my-trip-list"
                aria-label={t("trips.heading")}
            >
                {trips.map((trip) => (
                    <li key={trip.tripId}>
                        <Card className="destination-card">
                            <div className="destination-card__cover">
                                <TravelArtwork scene={sceneOf(trip)} />
                                {trip.roomId !== null && <Badge label={t("trips.withRoom")} />}
                            </div>
                            <div className="destination-card__body">
                                <h2>{trip.title ?? t("trips.untitled")}</h2>
                                <TripMeta trip={trip} />
                            </div>
                        </Card>
                    </li>
                ))}
            </ul>
            <ul className="trip-mobile-list my-trip-list" aria-label={t("trips.heading")}>
                {trips.map((trip) => (
                    <li key={trip.tripId} className="travel-row">
                        <span className="travel-row__main">
                            <TravelArtwork scene={sceneOf(trip)} />
                            <span className="travel-row__body">
                                <strong>{trip.title ?? t("trips.untitled")}</strong>
                                <TripMeta trip={trip} />
                            </span>
                        </span>
                    </li>
                ))}
            </ul>
        </>
    );
}
