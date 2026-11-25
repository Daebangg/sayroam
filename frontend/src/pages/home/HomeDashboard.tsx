import { useEffect, useRef, useState } from "react";
import { motion, useReducedMotion } from "motion/react";
import { useTranslation } from "react-i18next";
import { Badge, Button, Card, Icon } from "@/components/ui";
import { RecruitmentStatusBadge } from "@/components/features/matching/RecruitmentStatusBadge";
import { DetailsSidebar } from "@/components/layout/DetailsSidebar";
import { formatDateRange } from "@/i18n/locale";

const trips = [
    { place: "busan", start: "2026-12-04", end: "2026-12-06", role: "owner", members: 3 },
    { place: "jeju", start: "2026-12-18", end: "2026-12-21", role: "member", members: 2 },
    { place: "seoul", start: "2026-12-26", end: "2026-12-27", role: "owner", members: 2 },
] as const;

type Trip = (typeof trips)[number];
type Item = { trip: Trip; status?: "MATCHED" | "ACTIVE" | "CLOSED" };
const sections = [
    { title: "trips", icon: "trips", items: trips.map((trip) => ({ trip })) },
    {
        title: "matches",
        icon: "matching",
        items: [
            { trip: trips[1], status: "MATCHED" },
            { trip: trips[0], status: "ACTIVE" },
            { trip: trips[2], status: "CLOSED" },
        ],
    },
] as const satisfies readonly {
    title: string;
    icon: "trips" | "matching";
    items: readonly Item[];
}[];

export function HomeDashboard() {
    const { t, i18n } = useTranslation();
    const reduced = useReducedMotion();
    const [selected, setSelected] = useState<Item | null>(null);
    const [closing, setClosing] = useState(false);
    const dashboard = useRef<HTMLDivElement>(null);
    const [scrollable, setScrollable] = useState(false);
    useEffect(() => {
        const node = dashboard.current;
        if (!node) return;
        // 패널이 화면에 모두 들어온 뒤 카드 목록이 네이티브 스크롤을 이어받는다.
        const observer = new IntersectionObserver(
            ([entry]) => setScrollable(Boolean(entry && entry.intersectionRatio >= 0.99)),
            { threshold: 0.99 },
        );
        observer.observe(node);
        return () => observer.disconnect();
    }, []);
    const title = (item: Item) =>
        t(`home.dashboard.${item.status ? "matchTitles" : "tripTitles"}.${item.trip.place}`);
    const period = (trip: Trip) =>
        formatDateRange(i18n.resolvedLanguage ?? "ko", trip.start, trip.end);
    function badge({ trip, status }: Item) {
        return status ? (
            <RecruitmentStatusBadge status={status} />
        ) : (
            <Badge
                className="recruitment-status-badge"
                label={t(`home.dashboard.${trip.role}`)}
                tone={trip.role === "owner" ? "brand" : "neutral"}
            />
        );
    }
    function select(item: Item) {
        setClosing(selected === item && !closing);
        setSelected(item);
    }

    return (
        <div
            ref={dashboard}
            className="home-dashboard"
            id="home-dashboard"
            role="region"
            aria-labelledby="home-trips home-matches"
            tabIndex={0}
            data-scrollable={scrollable || undefined}
        >
            <div
                className={`panel-list-layout panel-list-layout--animated${selected && !closing ? " panel-list-layout--open" : ""}`}
            >
                {sections.map((section) => (
                    <section key={section.title} aria-labelledby={`home-${section.title}`}>
                        <h2 id={`home-${section.title}`} className="home-dashboard__heading">
                            <Icon name={section.icon} />
                            <span className="home-dashboard__wordmark">
                                {t(`home.dashboard.${section.title}`)}
                            </span>
                        </h2>
                        <ul className="home-dashboard__cards">
                            {section.items.map((item: Item, index) => (
                                <motion.li
                                    key={item.trip.place}
                                    initial={reduced ? false : { opacity: 0, y: 16 }}
                                    whileInView={{ opacity: 1, y: 0 }}
                                    viewport={{ once: true }}
                                    transition={{
                                        duration: reduced ? 0 : 0.32,
                                        delay: reduced ? 0 : index * 0.065,
                                    }}
                                >
                                    <Card
                                        className={`matching-registration home-dashboard__card${item.status === "MATCHED" ? " home-dashboard__card--matched" : ""}${selected === item && !closing ? " is-selected" : ""}`}
                                    >
                                        {badge(item)}
                                        <h3>
                                            <Button
                                                variant="plain"
                                                className="matching-registration__open"
                                                aria-expanded={selected === item && !closing}
                                                aria-controls={
                                                    selected === item
                                                        ? "home-details-panel"
                                                        : undefined
                                                }
                                                onClick={() => select(item)}
                                            >
                                                {title(item)}
                                            </Button>
                                        </h3>
                                        <p>
                                            <Icon name="journey-place" />
                                            {t(`home.dashboard.places.${item.trip.place}`)}
                                        </p>
                                        <p>
                                            <Icon name="journey-dates" />
                                            {period(item.trip)}
                                        </p>
                                        {!item.status && (
                                            <p>
                                                <Icon name="journey-people" />
                                                {t("home.dashboard.people", {
                                                    count: item.trip.members,
                                                })}
                                            </p>
                                        )}
                                    </Card>
                                </motion.li>
                            ))}
                        </ul>
                    </section>
                ))}
            </div>
            {selected && (
                <DetailsSidebar
                    id="home-details-panel"
                    titleId="home-details-title"
                    closing={closing}
                    onClose={() => setClosing(true)}
                    onExited={() => setSelected(null)}
                >
                    <div className="matching-details-panel__body" key={title(selected)}>
                        <header className="matching-details-panel__header">
                            {badge(selected)}
                            <h2 id="home-details-title" aria-live="polite">
                                {title(selected)}
                            </h2>
                            <div className="matching-details-introduction">
                                <p>
                                    {t(
                                        `home.dashboard.${selected.status ? "matchNotes" : "tripNotes"}.${selected.trip.place}`,
                                    )}
                                </p>
                            </div>
                        </header>
                        <dl className="matching-details-panel__meta">
                            <div>
                                <dt>
                                    <Icon name="pin" />
                                    {t("matchingFlow.destination")}
                                </dt>
                                <dd>{t(`home.dashboard.places.${selected.trip.place}`)}</dd>
                            </div>
                            <div>
                                <dt>
                                    <Icon name="calendar" />
                                    {t("matchingFlow.period")}
                                </dt>
                                <dd>{period(selected.trip)}</dd>
                            </div>
                            {!selected.status && (
                                <div>
                                    <dt>
                                        <Icon name="journey-people" />
                                        {t("mockup.travelers")}
                                    </dt>
                                    <dd>
                                        {t("home.dashboard.people", {
                                            count: selected.trip.members,
                                        })}
                                    </dd>
                                </div>
                            )}
                        </dl>
                    </div>
                    <footer className="matching-details-panel__footer">
                        <Button variant="close" onClick={() => setClosing(true)}>
                            {t("common.close")}
                        </Button>
                    </footer>
                </DetailsSidebar>
            )}
        </div>
    );
}
