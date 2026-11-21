import { useId, useState } from "react";
import { useNavigate, useOutletContext } from "react-router";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink, Card, DateRangeCalendar, Icon, TextField } from "@/components/ui";
import {
    FeaturedTrip,
    RoomPreviewRow,
    TravelRow,
} from "@/components/features/travel/TravelPreview";
import { PreviewDialog } from "@/components/features/travel/PreviewDialog";
import { MatchingFilterPopover } from "@/components/features/matching/MatchingFilterPopover";
import { matchingPreviews, roomPreviews, tripPreviews, type PreviewTrip } from "@/data/preview";
import { formatMonthDay } from "@/i18n/locale";
import { useCreateTrip } from "@/hooks/api/trips/useCreateTrip";
import { usePreviewNavigation } from "@/hooks/usePreviewNavigation";
import { pagePaths } from "@/routing/paths";
import type { PageContext } from "@/routing/context";

const shortcuts = [
    { id: "plan", icon: "trips", screen: "trips" },
    { id: "company", icon: "matching", screen: "matching" },
    { id: "destinations", icon: "pin", screen: "trips" },
    { id: "guide", icon: "guide", screen: null },
] as const;

// 여행 이름은 30자까지 저장된다. 영어 기본 이름 "Trip to "(8자)를 붙여도 넘지 않게 목적지를 제한한다.
const DESTINATION_MAX_LENGTH = 22;

export function HomePage() {
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const { isPreview, pathFor } = usePreviewNavigation();
    const [featuredIndex, setFeaturedIndex] = useState(0);
    const [saved, setSaved] = useState<string[]>([]);
    const [selected, setSelected] = useState<PreviewTrip | null>(null);
    const [guide, setGuide] = useState(false);
    const [schedule, setSchedule] = useState({ start: "", end: "" });
    const [missing, setMissing] = useState<"destination" | "schedule" | null>(null);
    const searchErrorId = useId();
    const destinationMissing = missing === "destination";
    const creation = useCreateTrip();
    const language = i18n.resolvedLanguage ?? "ko";
    const nights = schedule.end
        ? Math.round((Date.parse(schedule.end) - Date.parse(schedule.start)) / 86_400_000)
        : 0;
    const scheduleLabel = !schedule.start
        ? t("controls.datePicker.placeholder")
        : !schedule.end
          ? t("mockup.tripScheduleStart", { start: formatMonthDay(language, schedule.start) })
          : t("mockup.tripScheduleRange", {
                start: formatMonthDay(language, schedule.start),
                end: formatMonthDay(language, schedule.end),
                duration:
                    nights === 0
                        ? t("mockup.dayTrip")
                        : t("mockup.tripNights", { nights, days: nights + 1 }),
            });
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
                    if (isPreview) {
                        void navigate(pathFor("trips"));
                        return;
                    }
                    if (!member) {
                        onOpenAuth();
                        return;
                    }
                    const destination = String(
                        new FormData(event.currentTarget).get("destination") ?? "",
                    ).trim();
                    if (!destination) {
                        setMissing("destination");
                        return;
                    }
                    if (!schedule.end) {
                        setMissing("schedule");
                        return;
                    }
                    // 목적지는 저장하지 않고 화면 언어로 만든 기본 여행 이름에만 쓴다.
                    const title = t("mockup.defaultTripTitle", { destination });
                    void creation.create(schedule.start, schedule.end, title).then((tripId) => {
                        if (tripId !== null) void navigate(pagePaths.trips);
                    });
                }}
            >
                <div className="travel-search__destination">
                    <Icon name="pin" />
                    <TextField
                        label={t("mockup.destination")}
                        placeholder={t("mockup.destinationPlaceholder")}
                        name="destination"
                        maxLength={DESTINATION_MAX_LENGTH}
                        aria-invalid={destinationMissing || undefined}
                        aria-describedby={destinationMissing ? searchErrorId : undefined}
                        onChange={() => {
                            if (missing === "destination") setMissing(null);
                        }}
                    />
                </div>
                <div className="travel-search__schedule">
                    <span className="travel-search__label">{t("mockup.tripSchedule")}</span>
                    <MatchingFilterPopover
                        className="travel-search__schedule-trigger"
                        panelClassName="travel-search__calendar"
                        placement="top"
                        icon="calendar"
                        label={scheduleLabel}
                    >
                        {(close) => (
                            <DateRangeCalendar
                                label={t("mockup.tripSchedule")}
                                startDate={schedule.start}
                                endDate={schedule.end}
                                summary={false}
                                onChange={(start, end) => {
                                    setSchedule({ start, end });
                                    if (missing === "schedule") setMissing(null);
                                    if (end) close();
                                }}
                            />
                        )}
                    </MatchingFilterPopover>
                </div>
                <Button type="submit" className="travel-search__submit" loading={creation.busy}>
                    {t("mockup.planTrip")}
                </Button>
            </form>
            {(missing || creation.failed) && (
                <p id={searchErrorId} className="travel-search-error" role="alert">
                    {t(
                        destinationMissing
                            ? "mockup.destinationRequired"
                            : missing === "schedule"
                              ? "mockup.scheduleRequired"
                              : "mockup.planTripFailed",
                    )}
                </p>
            )}
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
