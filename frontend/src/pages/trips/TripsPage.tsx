import { useState } from "react";
import { useOutletContext, useSearchParams } from "react-router";
import { useTranslation } from "react-i18next";
import { Badge, Button, Card, FilterChips, Icon, PageState, TravelArtwork } from "@/components/ui";
import { MyTripList } from "@/components/features/travel/MyTripList";
import { TravelMeta, TravelRow } from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { tripPreviews, type PreviewTrip } from "@/data/preview";
import { useMyTrips } from "@/hooks/api/trips/useMyTrips";
import { usePreviewNavigation } from "@/hooks/usePreviewNavigation";
import type { PageContext } from "@/routing/context";

export function TripsPage() {
    const { isPreview } = usePreviewNavigation();
    return isPreview ? <TripsPreview /> : <MemberTrips />;
}

// 회원 전용 경로라 App의 MemberGate가 로그인 전 진입을 막는다.
function MemberTrips() {
    const { member } = useOutletContext<PageContext>();
    const { t } = useTranslation();
    const [creating, setCreating] = useState(false);
    return (
        <main id="main-content" tabIndex={-1} className="workspace-page travel-workspace">
            <div className="page-heading">
                <h1>{t("trips.heading")}</h1>
                <Button variant="outline" onClick={() => setCreating(true)}>
                    <Icon name="plus" variant="outline" />
                    {t("trips.create")}
                </Button>
            </div>
            {member && <MyTrips key={member.userId} userId={member.userId} />}
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

function MyTrips({ userId }: { userId: number }) {
    const { t } = useTranslation();
    const { trips, status, reload } = useMyTrips(userId);
    if (status === "loading")
        return (
            <PageState
                kind="loading"
                headingAs="h3"
                title={t("trips.loading")}
                description={t("trips.loading")}
            />
        );
    if (status === "failed")
        return (
            <PageState
                kind="error"
                headingAs="h3"
                title={t("trips.loadErrorTitle")}
                description={t("trips.loadErrorDescription")}
                actions={<Button onClick={reload}>{t("trips.retry")}</Button>}
            />
        );
    if (trips.length === 0)
        return (
            <div className="travel-empty">
                <TravelArtwork />
                <h2>{t("trips.emptyTitle")}</h2>
                <p>{t("trips.emptyDescription")}</p>
            </div>
        );
    return <MyTripList trips={trips} />;
}

function TripsPreview() {
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
