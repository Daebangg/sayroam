export type Language = "ko" | "en";

export function normalizeLanguage(value: unknown): Language {
    return value === "en" ? "en" : "ko";
}

function locale(language: string): string {
    return normalizeLanguage(language) === "en" ? "en-US" : "ko-KR";
}

export function formatDateRange(language: string, start: string, end: string): string {
    return new Intl.DateTimeFormat(locale(language), {
        year: "numeric",
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    }).formatRange(new Date(start), new Date(end));
}

/** 짧은 일정 표시용. 연도를 빼고 월·일만 보여 준다. */
export function formatMonthDay(language: string, value: string): string {
    return new Intl.DateTimeFormat(locale(language), {
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    }).format(new Date(value));
}

export function formatNumber(language: string, value: number): string {
    return new Intl.NumberFormat(locale(language)).format(value);
}

export function formatDate(language: string, value: string): string {
    return new Intl.DateTimeFormat(locale(language), {
        year: "numeric",
        month: "long",
        day: "numeric",
    }).format(new Date(value));
}

/** 일정 머리글용. 월·일과 요일을 함께 보여 준다. */
export function formatDayDate(language: string, value: string): string {
    return new Intl.DateTimeFormat(locale(language), {
        month: "short",
        day: "numeric",
        weekday: "short",
        timeZone: "UTC",
    }).format(new Date(value));
}

/** 좁은 칸에 두 줄로 쓰도록 월과 일을 나눈다. 예: "12월" / "2일", "Dec" / "2" */
export function formatMonthDayParts(
    language: string,
    value: string,
): { month: string; day: string } {
    const parts = new Intl.DateTimeFormat(locale(language), {
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    }).formatToParts(new Date(value));
    const month = parts.find((part) => part.type === "month")?.value ?? "";
    const index = parts.findIndex((part) => part.type === "day");
    const suffix =
        parts[index + 1]?.type === "literal" ? (parts[index + 1]?.value.trim() ?? "") : "";
    return { month, day: `${parts[index]?.value ?? ""}${suffix}` };
}

export function formatTime(language: string, value: Date): string {
    return new Intl.DateTimeFormat(locale(language), { hour: "numeric", minute: "2-digit" }).format(
        value,
    );
}
