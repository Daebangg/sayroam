import { useTranslation } from "react-i18next";
import { Button, Icon, Tag } from "@/components/ui";
import { tripPreviews } from "@/data/preview";

export function TripsPage() {
    const { t } = useTranslation();

    return (
        <main className="workspace-page">
            <div className="page-heading">
                <h1>{t("trips.heading")}</h1>
                <Button
                    variant="ghost"
                    className="icon-button page-heading__mobile-action"
                    aria-label={t("trips.create")}
                >
                    <Icon name="plus" />
                </Button>
            </div>
            <div className="trip-grid">
                {tripPreviews.map((trip) => (
                    <article className="trip-card" key={trip.id}>
                        <img src={trip.image} alt={t(trip.title)} />
                        <div className="trip-card__body">
                            <h2>{t(trip.title)}</h2>
                            <div className="trip-card__tags">
                                <Tag
                                    label={t(
                                        trip.id === "swiss" ? "brand.tagWinter" : "brand.tagFree",
                                    )}
                                    tone="primary"
                                />
                                <Tag label={t("brand.planning")} />
                            </div>
                            <div className="trip-card__meta">
                                <p>{t(trip.dates)}</p>
                                <Icon name="matching" />
                            </div>
                        </div>
                    </article>
                ))}
                <button type="button" className="new-trip-card">
                    <span className="new-trip-card__icon">
                        <Icon name="plus" />
                    </span>
                    <span>{t("trips.create")}</span>
                </button>
            </div>
        </main>
    );
}
