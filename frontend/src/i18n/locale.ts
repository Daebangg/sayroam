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
