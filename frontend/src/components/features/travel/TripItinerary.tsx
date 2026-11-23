import { useEffect, useEffectEvent, useRef, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";
import { formatDayDate } from "@/i18n/locale";
import type { TripPlace } from "@/hooks/trip.types";
import { tripPlaceId, tripSectionId, type TripSection } from "./tripDays";

type TripItineraryProps = {
    sections: TripSection[];
    highlightedPlaceId: number | null;
    onFocusPlace: (placeId: number) => void;
    /** 화면에서 지금 읽고 있는 일차가 바뀌면 알린다. */
    onActiveChange: (key: string) => void;
    addingKey: string | null;
    onStartAdd: (key: string) => void;
    /** 장소를 추가 중인 일차에 띄우는 추가 상자 */
    addBox: ReactNode;
};

function PlaceRow({
    place,
    order,
    tone,
    highlighted,
    onFocus,
}: {
    place: TripPlace;
    order: number | null;
    tone: number | null;
    highlighted: boolean;
    onFocus: () => void;
}) {
    const { t } = useTranslation();
    const category = t(`tripDetail.category.${place.category ?? "CUSTOM"}`);
    const detail = place.memo ?? place.address;
    return (
        <li id={tripPlaceId(place.tripPlaceId)}>
            <Button
                variant="plain"
                className={`trip-place${highlighted ? " is-highlighted" : ""}`}
                aria-label={t("tripDetail.focusPlace", { name: place.name })}
                onClick={onFocus}
            >
                <span
                    className={`trip-place__pin ${tone === null ? "trip-pin--unscheduled" : `trip-pin--tone-${tone}`}`}
                    aria-hidden="true"
                >
                    {order ?? "·"}
                </span>
                <span className="trip-place__body">
                    <span className="trip-place__name">{place.name}</span>
                    <span className="trip-place__note">
                        {detail ? `${category} · ${detail}` : category}
                    </span>
                </span>
            </Button>
        </li>
    );
}

export function TripItinerary({
    sections,
    highlightedPlaceId,
    onFocusPlace,
    onActiveChange,
    addingKey,
    onStartAdd,
    addBox,
}: TripItineraryProps) {
    const { t, i18n } = useTranslation();
    const language = i18n.resolvedLanguage ?? "ko";
    const root = useRef<HTMLElement>(null);
    const notifyActive = useEffectEvent(onActiveChange);

    // 화면 위쪽 40% 안에 들어온 첫 일차를 지금 읽는 일차로 본다.
    useEffect(() => {
        const elements = [...(root.current?.querySelectorAll<HTMLElement>("[data-section]") ?? [])];
        const showing = new Set<string>();
        const observer = new IntersectionObserver(
            (entries) => {
                for (const entry of entries) {
                    const key = (entry.target as HTMLElement).dataset.section ?? "";
                    if (entry.isIntersecting) showing.add(key);
                    else showing.delete(key);
                }
                const first = elements.find((element) =>
                    showing.has(element.dataset.section ?? ""),
                );
                if (first?.dataset.section) notifyActive(first.dataset.section);
            },
            { rootMargin: "0px 0px -60% 0px" },
        );
        for (const element of elements) observer.observe(element);
        return () => observer.disconnect();
    }, [sections]);

    return (
        <section ref={root} className="trip-itinerary" aria-labelledby="trip-itinerary-title">
            <h2 id="trip-itinerary-title">{t("tripDetail.itinerary")}</h2>
            {sections.map((section) => {
                const unscheduled = section.day === null;
                return (
                    <div
                        key={section.key}
                        id={tripSectionId(section.key)}
                        data-section={section.key}
                        className={`trip-day${unscheduled ? " trip-day--unscheduled" : ""}`}
                    >
                        <div className="trip-day__heading">
                            <h3>
                                {unscheduled
                                    ? t("tripDetail.unscheduled")
                                    : t("tripDetail.dayTitle", { day: section.day })}
                            </h3>
                            {section.date && <span>{formatDayDate(language, section.date)}</span>}
                        </div>
                        {section.places.length > 0 ? (
                            <ol className="trip-day__places">
                                {section.places.map((place, index) => (
                                    <PlaceRow
                                        key={place.tripPlaceId}
                                        place={place}
                                        order={unscheduled ? null : index + 1}
                                        tone={section.tone}
                                        highlighted={place.tripPlaceId === highlightedPlaceId}
                                        onFocus={() => onFocusPlace(place.tripPlaceId)}
                                    />
                                ))}
                            </ol>
                        ) : (
                            <p className="trip-day__empty">
                                {t(
                                    unscheduled
                                        ? "tripDetail.unscheduledEmpty"
                                        : "tripDetail.emptyDay",
                                )}
                            </p>
                        )}
                        {!unscheduled &&
                            (addingKey === section.key ? (
                                addBox
                            ) : (
                                <Button
                                    variant="plain"
                                    className="trip-day__add"
                                    onClick={() => onStartAdd(section.key)}
                                >
                                    <Icon name="plus" variant="outline" />
                                    {t("tripDetail.addPlace")}
                                </Button>
                            ))}
                    </div>
                );
            })}
        </section>
    );
}
