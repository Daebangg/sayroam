import { useState } from "react";
import { useNavigate, useOutletContext } from "react-router";
import { useTranslation } from "react-i18next";
import {
    Button,
    ButtonLink,
    Card,
    DatePicker,
    Icon,
    NumberStepper,
    TextField,
} from "@/components/ui";
import {
    FeaturedTrip,
    RoomPreviewRow,
    TravelRow,
} from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { matchingPreviews, roomPreviews, tripPreviews, type PreviewTrip } from "@/data/preview";
import { usePreviewNavigation } from "@/hooks/usePreviewNavigation";
import type { PageContext } from "@/routing/context";

const shortcuts = [
    { id: "plan", icon: "trips", screen: "trips" },
    { id: "company", icon: "matching", screen: "matching" },
    { id: "destinations", icon: "pin", screen: "trips" },
    { id: "guide", icon: "guide", screen: null },
] as const;

export function HomePage() {
    const { onOpenAuth } = useOutletContext<PageContext>();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const { isPreview, pathFor } = usePreviewNavigation();
    const [featuredIndex, setFeaturedIndex] = useState(0);
    const [saved, setSaved] = useState<string[]>([]);
    const [selected, setSelected] = useState<PreviewTrip | null>(null);
    const [guide, setGuide] = useState(false);
    const [departure, setDeparture] = useState("");
    const [travelers, setTravelers] = useState(2);
    const featured = tripPreviews[featuredIndex]!;
    const toggle = (id: string): void =>
        setSaved((values) =>
            values.includes(id) ? values.filter((value) => value !== id) : [...values, id],
        );
    return (
        <main id="main-content" tabIndex={-1} className="dashboard-page">
            <section className="travel-hero" aria-labelledby="home-heading">
                <img
                    className="travel-hero__image"
                    src="/images/travel-coast.jpg"
                    alt=""
                    fetchPriority="high"
                />
                <div className="travel-hero__copy">
                    <h1 id="home-heading">
                        {t("home.titleFirst")}
                        <br />
                        {t("home.titleSecond")}
                    </h1>
                    <p>
                        {t("home.descriptionFirst")}
                        <br />
                        {t("home.descriptionSecond")}
                    </p>
                    {isPreview ? (
                        <ButtonLink to={pathFor("trips")} variant="primary">
                            {t("home.start")}
                            <Icon name="arrow" variant="outline" />
                        </ButtonLink>
                    ) : (
                        <Button onClick={onOpenAuth}>
                            {t("home.start")}
                            <Icon name="arrow" variant="outline" />
                        </Button>
                    )}
                </div>
            </section>
            <form
                className="travel-search"
                onSubmit={(event) => {
                    event.preventDefault();
                    const form = new FormData(event.currentTarget);
                    const query = String(form.get("destination") ?? "").trim();
                    void navigate(
                        pathFor("trips") +
                            (isPreview ? "&" : "?") +
                            "q=" +
                            encodeURIComponent(query),
                    );
                }}
            >
                <div className="travel-search__destination">
                    <Icon name="pin" />
                    <TextField
                        label={t("mockup.destination")}
                        placeholder={t("mockup.destinationPlaceholder")}
                        name="destination"
                        maxLength={50}
                    />
                </div>
                <DatePicker
                    label={t("mockup.departure")}
                    name="departure"
                    value={departure}
                    onChange={setDeparture}
                />
                <NumberStepper
                    label={t("mockup.travelers")}
                    name="travelers"
                    value={travelers}
                    onChange={setTravelers}
                    min={1}
                    max={20}
                />
                <Button
                    type="submit"
                    className="travel-search__submit"
                    aria-label={t("common.search")}
                >
                    <Icon name="search" variant="outline" />
                </Button>
            </form>
            <nav className="travel-shortcuts" aria-label={t("mockup.shortcuts")}>
                {shortcuts.map((item) => {
                    const content = (
                        <>
                            <span className={`feature-icon feature-icon--${item.id}`}>
                                <Icon name={item.icon} />
                            </span>
                            <span>
                                <strong>{t(`mockup.shortcutsContent.${item.id}.title`)}</strong>
                                <span>{t(`mockup.shortcutsContent.${item.id}.description`)}</span>
                            </span>
                            <Icon name="chevron" variant="outline" />
                        </>
                    );
                    return item.screen ? (
                        <ButtonLink
                            key={item.id}
                            to={pathFor(item.screen)}
                            variant="plain"
                            className="shortcut-card"
                        >
                            {content}
                        </ButtonLink>
                    ) : (
                        <Button
                            key={item.id}
                            variant="plain"
                            className="shortcut-card"
                            onClick={() => setGuide(true)}
                        >
                            {content}
                        </Button>
                    );
                })}
            </nav>
            <div className="dashboard-grid">
                <Card className="dashboard-section dashboard-section--featured">
                    <div className="section-heading">
                        <h2>{t("mockup.popularTrips")}</h2>
                        <ButtonLink
                            variant="ghost"
                            className="icon-button"
                            to={pathFor("trips")}
                            aria-label={t("mockup.viewTrips")}
                        >
                            <Icon name="arrow" variant="outline" />
                        </ButtonLink>
                    </div>
                    <FeaturedTrip trip={featured} onOpen={() => setSelected(featured)} />
                    <div
                        className="carousel-dots"
                        role="group"
                        aria-label={t("mockup.popularTrips")}
                    >
                        {tripPreviews.slice(0, 4).map((trip, index) => (
                            <Button
                                key={trip.id}
                                variant="plain"
                                aria-label={t("mockup.showTrip", { title: t(trip.title) })}
                                aria-pressed={featuredIndex === index}
                                onClick={() => setFeaturedIndex(index)}
                            />
                        ))}
                    </div>
                </Card>
                <Card className="dashboard-section">
                    <div className="section-heading">
                        <h2>{t("mockup.recruiting")}</h2>
                        <ButtonLink
                            variant="ghost"
                            className="icon-button"
                            to={pathFor("matching")}
                            aria-label={t("mockup.viewMatching")}
                        >
                            <Icon name="arrow" variant="outline" />
                        </ButtonLink>
                    </div>
                    <div className="travel-preview-list">
                        {matchingPreviews.map((trip) => (
                            <TravelRow
                                key={trip.id}
                                trip={trip}
                                onOpen={() => setSelected(trip)}
                                saved={saved.includes(trip.id)}
                                onSave={() => toggle(trip.id)}
                            />
                        ))}
                    </div>
                </Card>
                <Card className="dashboard-section">
                    <div className="section-heading">
                        <h2>{t("mockup.myRooms")}</h2>
                        <ButtonLink
                            variant="ghost"
                            className="icon-button"
                            to={pathFor("rooms")}
                            aria-label={t("mockup.viewRooms")}
                        >
                            <Icon name="arrow" variant="outline" />
                        </ButtonLink>
                    </div>
                    <div className="travel-preview-list">
                        {roomPreviews.map((room) => (
                            <RoomPreviewRow
                                key={room.id}
                                room={room}
                                onOpen={() => {
                                    void navigate(pathFor("rooms"));
                                }}
                            />
                        ))}
                    </div>
                </Card>
            </div>
            <p className="dashboard-footnote">{t("mockup.sampleData")}</p>
            <PreviewDialog
                open={selected !== null}
                title={selected ? t(selected.title) : ""}
                description={t("mockup.tripPreviewDescription")}
                scene={selected?.scene ?? "mountain"}
                onClose={() => setSelected(null)}
            />
            <PreviewDialog
                open={guide}
                title={t("mockup.guideTitle")}
                description={t("mockup.guideDescription")}
                scene="coast"
                onClose={() => setGuide(false)}
            />
        </main>
    );
}
