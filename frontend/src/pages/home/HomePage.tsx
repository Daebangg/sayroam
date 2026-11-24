import { useEffect, useId, useState } from "react";
import { useNavigate, useOutletContext } from "react-router";
import { useTranslation } from "react-i18next";
import { Autocomplete, Button, DateRangeCalendar, Icon } from "@/components/ui";
import { MatchingFilterPopover } from "@/components/features/matching/MatchingFilterPopover";
import { formatMonthDay } from "@/i18n/locale";
import { useDestinations } from "@/hooks/api/destinations/useDestinations";
import { useCreateTrip } from "@/hooks/api/trips/useCreateTrip";
import { usePreviewNavigation } from "@/hooks/usePreviewNavigation";
import { tripDetailPath } from "@/routing/paths";
import { DestinationSlider } from "@/components/features/travel/DestinationSlider";
import type { PageContext } from "@/routing/context";

export function HomePage() {
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const { t, i18n } = useTranslation();
    const navigate = useNavigate();
    const { isPreview, pathFor } = usePreviewNavigation();
    const [compactCalendar, setCompactCalendar] = useState(
        () => window.matchMedia("(max-width: 700px)").matches,
    );
    useEffect(() => {
        const media = window.matchMedia("(max-width: 700px)");
        const sync = () => setCompactCalendar(media.matches);
        media.addEventListener("change", sync);
        sync();
        return () => media.removeEventListener("change", sync);
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
        <main id="main-content" tabIndex={-1} className="home-planner">
            <header className="home-planner__intro">
                <h1>
                    {t("home.titleFirst")} <span>{t("home.titleSecond")}</span>
                </h1>
                <p>
                    {t("home.descriptionFirst")} <span>{t("home.descriptionSecond")}</span>
                </p>
            </header>
            <div className="home-planner__content">
                <form
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
                                    monthCount={compactCalendar ? 1 : 2}
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
            </div>
            <DestinationSlider />
        </main>
    );
}
