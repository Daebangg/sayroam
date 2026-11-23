import type { TripPlace } from "@/hooks/trip.types";

/** 일정 한 칸. day가 null이면 일차 미정 장소 묶음이다. */
export type TripSection = {
    key: string;
    day: number | null;
    /** 일차의 날짜(YYYY-MM-DD). 미정 묶음은 null */
    date: string | null;
    /** 핀 색. 일차 순서대로 돌려 쓴다. 미정 묶음은 null */
    tone: number | null;
    places: TripPlace[];
};

export const DAY_TONES = 5;
const DAY_MS = 86_400_000;

function addDays(date: string, days: number): string {
    return new Date(Date.parse(`${date}T00:00:00Z`) + days * DAY_MS).toISOString().slice(0, 10);
}

/** 여행 기간의 일차와 미정 묶음을 만들고 장소를 나눠 담는다. 기간 밖 일차의 장소는 미정에 둔다. */
export function tripSections(
    startDate: string,
    endDate: string,
    places: TripPlace[],
): TripSection[] {
    const length =
        Math.round(
            (Date.parse(`${endDate}T00:00:00Z`) - Date.parse(`${startDate}T00:00:00Z`)) / DAY_MS,
        ) + 1;
    const days: TripSection[] = Array.from({ length }, (_, index) => ({
        key: `day-${index + 1}`,
        day: index + 1,
        date: addDays(startDate, index),
        tone: index % DAY_TONES,
        places: [],
    }));
    const unscheduled: TripSection = {
        key: "unscheduled",
        day: null,
        date: null,
        tone: null,
        places: [],
    };
    for (const place of places) {
        const section = place.day === null ? undefined : days[place.day - 1];
        (section ?? unscheduled).places.push(place);
    }
    return [...days, unscheduled];
}

export function tripSectionId(key: string): string {
    return `trip-section-${key}`;
}

export function tripPlaceId(placeId: number): string {
    return `trip-place-${placeId}`;
}
