import { useEffect, useId, useRef, useState } from "react";
import { motion, useMotionValueEvent, useReducedMotion, useScroll } from "motion/react";
import { HomeDashboard } from "./HomeDashboard";
import { useNavigate, useOutletContext } from "react-router";
import { useTranslation } from "react-i18next";
import { Autocomplete, Button, DateRangeCalendar, Icon } from "@/components/ui";
import { MatchingFilterPopover } from "@/components/features/matching/MatchingFilterPopover";
import { formatMonthDay } from "@/i18n/locale";
import { useDestinations } from "@/hooks/api/destinations/useDestinations";
import { useCreateTrip } from "@/hooks/api/trips/useCreateTrip";
import { usePreviewNavigation } from "@/hooks/usePreviewNavigation";
import { tripDetailPath } from "@/routing/paths";
import { DestinationPhoto } from "@/components/features/travel/DestinationPhoto";
import type { PageContext } from "@/routing/context";

export function HomePage() {
    const hero = useRef<HTMLElement>(null);
    const { scrollYProgress } = useScroll({ target: hero, offset: ["start start", "end start"] });
    const [overDashboard, setOverDashboard] = useState(false);
    const [docked, setDocked] = useState(false);
    const reduceMotion = useReducedMotion();
    useMotionValueEvent(scrollYProgress, "change", (progress) => {
        setDocked(progress >= 0.28);
        setOverDashboard(progress >= 0.93);
    });
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const { isPreview, pathFor } = usePreviewNavigation();
    const searchForm = useRef<HTMLFormElement>(null);
    const [compactCalendar, setCompactCalendar] = useState(false);
    useEffect(() => {
        const form = searchForm.current;
        if (!form) return;
        const observer = new ResizeObserver(([entry]) => {
            if (entry) setCompactCalendar(entry.contentRect.width < 560);
        });
        observer.observe(form);
        return () => observer.disconnect();
    }, []);
    const [schedule, setSchedule] = useState({ start: "", end: "" });
    const [destinationQuery, setDestinationQuery] = useState("");
    const [destinationId, setDestinationId] = useState("");
    const [missing, setMissing] = useState<"destination" | "schedule" | null>(null);
    const searchErrorId = useId();
    const destinationMissing = missing === "destination";
    const creation = useCreateTrip();
    const catalog = useDestinations(member?.userId);
    const destination = catalog.destinations.find((item) => String(item.id) === destinationId);
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
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className="home-planner"
            data-docked={docked || undefined}
            data-over-dashboard={overDashboard || undefined}
        >
            <div className="home-planner__rail">
                <motion.div
                    className="home-planner__content"
                    layout={!reduceMotion}
                    layoutDependency={docked}
                    transition={{ layout: { duration: 0.36, ease: [0.22, 1, 0.36, 1] } }}
                >
                    <form
                        ref={searchForm}
                        className="travel-search"
                        aria-describedby={missing || creation.failed ? searchErrorId : undefined}
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
                            if (!destination) {
                                setMissing("destination");
                                return;
                            }
                            if (!schedule.end) {
                                setMissing("schedule");
                                return;
                            }
                            // 고른 여행지로 화면 언어의 기본 여행 이름을 만든다. 여행지는 상세 지도의 첫 화면에 쓰인다.
                            const title = t("mockup.defaultTripTitle", {
                                destination: destination.name,
                            });
                            void creation
                                .create(destination.id, schedule.start, schedule.end, title)
                                .then((tripId) => {
                                    if (tripId !== null) void navigate(tripDetailPath(tripId));
                                });
                        }}
                    >
                        <div className="travel-search__destination">
                            <Icon name="pin" />
                            <Autocomplete
                                label={t("mockup.destination")}
                                placeholder={t("mockup.destinationPlaceholder")}
                                value={destinationId}
                                query={destinationQuery}
                                options={catalog.destinations.map((item) => ({
                                    value: String(item.id),
                                    label: item.name,
                                }))}
                                emptyLabel={t(
                                    !member
                                        ? "mockup.destinationSignIn"
                                        : catalog.status === "failed"
                                          ? "mockup.destinationsUnavailable"
                                          : "mockup.noDestinations",
                                )}
                                onQueryChange={(value) => {
                                    setDestinationQuery(value);
                                    setDestinationId("");
                                }}
                                onSelect={(option) => {
                                    setDestinationId(option.value);
                                    setDestinationQuery(option.label);
                                    if (missing === "destination") setMissing(null);
                                }}
                            />
                        </div>
                        <div className="travel-search__schedule">
                            <MatchingFilterPopover
                                anchorBounds={searchForm}
                                fieldLabel={t("mockup.tripSchedule")}
                                className="travel-search__schedule-trigger"
                                panelClassName="travel-search__calendar"
                                icon="calendar"
                                label={scheduleLabel}
                            >
                                {(close) => (
                                    <DateRangeCalendar
                                        label={t("mockup.tripSchedule")}
                                        startDate={schedule.start}
                                        endDate={schedule.end}
                                        monthCount={compactCalendar || docked ? 1 : 2}
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
                        <Button
                            type="submit"
                            variant="highlight"
                            className="travel-search__submit"
                            loading={creation.busy}
                        >
                            <span>{t("mockup.planTrip")}</span>
                            <Icon name="arrow" variant="outline" />
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
                </motion.div>
            </div>
            <section className="home-planner__hero" ref={hero} aria-labelledby="home-heading">
                <header className="home-planner__intro">
                    <h1 id="home-heading">
                        {t("home.titleFirst")} <span>{t("home.titleSecond")}</span>
                    </h1>
                    <p>
                        {t("home.descriptionFirst")} <span>{t("home.descriptionSecond")}</span>
                    </p>
                </header>
                <DestinationPhoto />
            </section>
            <HomeDashboard />
        </main>
    );
}
