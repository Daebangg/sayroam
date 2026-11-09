import { useState } from "react";
import { useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Badge, Button, Card, FilterChips, Icon, TravelArtwork } from "@/components/ui";
import { TravelMeta, TravelRow } from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { tripPreviews, type PreviewTrip } from "@/data/preview";

export function TripsPage() {
    const { t } = useTranslation();
    const [params, setParams] = useSearchParams();
    const [filter, setFilter] = useState("all");
    const [saved, setSaved] = useState<string[]>([]);
    const [selected, setSelected] = useState<PreviewTrip | null>(null);
    const [creating, setCreating] = useState(false);
    const query = params.get("q")?.trim() ?? "";
    const trips = tripPreviews.filter(
        (trip) =>
            (filter === "all" ||
                (filter === "workation" ? trip.workation : trip.region === filter)) &&
            (!query || t(trip.title).toLowerCase().includes(query.toLowerCase())),
    );
    return (
        <main id="main-content" tabIndex={-1} className="workspace-page travel-workspace">
            <div className="page-heading">
                <div>
                    <p className="page-eyebrow">{t("mockup.explore")}</p>
                    <h1>{t("navigation.trips")}</h1>
                </div>
                <Button variant="outline" onClick={() => setCreating(true)}>
                    <Icon name="plus" variant="outline" />
                    {t("trips.create")}
                </Button>
            </div>
            <FilterChips
                label={t("mockup.tripFilters")}
                value={filter}
                onChange={setFilter}
                options={["all", "domestic", "overseas", "workation"].map((value) => ({
                    value,
                    label: t(
                        `mockup.filters.${value as "all" | "domestic" | "overseas" | "workation"}`,
                    ),
                }))}
            />
            {query && (
                <p className="travel-results" role="status">
                    {t("mockup.searchResults", { query, count: trips.length })}
                </p>
            )}
            <div className="trip-grid trip-grid--desktop">
                {trips.map((trip) => (
                    <Card key={trip.id} className="destination-card">
                        <Button
                            variant="plain"
                            className="destination-card__cover"
                            aria-label={t(trip.title)}
                            onClick={() => setSelected(trip)}
                        >
                            <TravelArtwork scene={trip.scene} />
                            <Badge
                                label={t(
                                    trip.workation ? "mockup.filters.workation" : "brand.looking",
                                )}
                            />
                        </Button>
                        <div className="destination-card__body">
                            <h2>
                                <Button variant="plain" onClick={() => setSelected(trip)}>
                                    {t(trip.title)}
                                </Button>
                            </h2>
                            <TravelMeta trip={trip} />
                        </div>
                    </Card>
                ))}
            </div>
            <div className="trip-mobile-list">
                {trips.map((trip) => (
                    <TravelRow
                        key={trip.id}
                        trip={trip}
                        onOpen={() => setSelected(trip)}
                        saved={saved.includes(trip.id)}
                        onSave={() =>
                            setSaved((values) =>
                                values.includes(trip.id)
                                    ? values.filter((value) => value !== trip.id)
                                    : [...values, trip.id],
                            )
                        }
                    />
                ))}
            </div>
            {trips.length === 0 && (
                <div className="travel-empty">
                    <TravelArtwork />
                    <h2>{t("mockup.noResults")}</h2>
                    <Button
                        variant="outline"
                        onClick={() => {
                            setFilter("all");
                            const next = new URLSearchParams(params);
                            next.delete("q");
                            setParams(next);
                        }}
                    >
                        {t("mockup.filters.all")}
                    </Button>
                </div>
            )}
            <p className="dashboard-footnote">{t("mockup.sampleData")}</p>
            <PreviewDialog
                open={selected !== null}
                title={selected ? t(selected.title) : ""}
                description={t("mockup.tripPreviewDescription")}
                scene={selected?.scene ?? "mountain"}
                onClose={() => setSelected(null)}
            />
            <PreviewDialog
                open={creating}
                title={t("trips.create")}
                description={t("mockup.createTripDescription")}
                scene="coast"
                onClose={() => setCreating(false)}
            />
        </main>
    );
}
