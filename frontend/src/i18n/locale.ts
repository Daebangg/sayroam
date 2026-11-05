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

export function formatNumber(language: string, value: number): string {
    return new Intl.NumberFormat(locale(language)).format(value);
}
