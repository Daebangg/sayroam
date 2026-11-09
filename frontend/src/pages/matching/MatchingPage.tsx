import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, FilterChips, Icon, TravelArtwork } from "@/components/ui";
import { TravelRow } from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { matchingPreviews, type PreviewTrip } from "@/data/preview";

export function MatchingPage() {
    const { t } = useTranslation();
    const [view, setView] = useState("mine");
    const [saved, setSaved] = useState<string[]>([]);
    const [selected, setSelected] = useState<PreviewTrip | null>(null);
    const trips =
        view === "saved"
            ? matchingPreviews.filter((trip) => saved.includes(trip.id))
            : matchingPreviews;
    const empty = view === "mine" || trips.length === 0;
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className="workspace-page workspace-page--list travel-workspace"
        >
            <div className="page-heading">
                <div>
                    <p className="page-eyebrow">{t("mockup.together")}</p>
                    <h1>{t("matching.heading")}</h1>
                </div>
            </div>
            <FilterChips
                label={t("matching.heading")}
                value={view}
                onChange={setView}
                options={[
                    { value: "mine", label: t("mockup.myCompanions") },
                    { value: "browse", label: t("brand.looking") },
                    { value: "saved", label: t("common.bookmark") },
                ]}
            />
            {empty ? (
                <div className="travel-empty">
                    <TravelArtwork />
                    <h2>{t("mockup.matchingEmptyTitle")}</h2>
                    <p>{t("mockup.matchingEmptyDescription")}</p>
                    <Button onClick={() => setView("browse")}>
                        <Icon name="search" variant="outline" />
                        {t("matching.heading")}
                    </Button>
                </div>
            ) : (
                <div className="travel-list-panel">
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
            )}
            <p className="dashboard-footnote">{t("mockup.sampleData")}</p>
            <PreviewDialog
                open={selected !== null}
                title={selected ? t(selected.title) : ""}
                description={t("mockup.tripPreviewDescription")}
                scene={selected?.scene ?? "mountain"}
                onClose={() => setSelected(null)}
            />
        </main>
    );
}
