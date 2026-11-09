const DAY_MILLISECONDS = 86_400_000;

export function parseCalendarDate(value: string | undefined): Date | undefined {
    if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return undefined;
    const [year = 0, month = 0, day = 0] = value.split("-").map(Number);
    if (year < 1 || year > 9999 || month < 1 || month > 12 || day < 1 || day > 31) return undefined;
    const date = new Date(0);
    date.setUTCHours(12, 0, 0, 0);
    date.setUTCFullYear(year, month - 1, day);
    return formatCalendarDate(date) === value ? date : undefined;
}

export function formatCalendarDate(date: Date): string {
    return [
        String(date.getUTCFullYear()).padStart(4, "0"),
        String(date.getUTCMonth() + 1).padStart(2, "0"),
        String(date.getUTCDate()).padStart(2, "0"),
    ].join("-");
}

export function calendarToday(): Date {
    const local = new Date();
    const date = new Date(0);
    date.setUTCHours(12, 0, 0, 0);
    date.setUTCFullYear(local.getFullYear(), local.getMonth(), local.getDate());
    return date;
}

export function addCalendarDays(date: Date, days: number): Date {
    return new Date(date.getTime() + days * DAY_MILLISECONDS);
}

export function calendarMonthStart(date: Date): Date {
    const start = new Date(date);
    start.setUTCDate(1);
    return start;
}

export function addCalendarMonths(date: Date, months: number): Date {
    const target = calendarMonthStart(date);
    target.setUTCMonth(target.getUTCMonth() + months);
    const last = new Date(target);
    last.setUTCMonth(last.getUTCMonth() + 1, 0);
    target.setUTCDate(Math.min(date.getUTCDate(), last.getUTCDate()));
    return target;
}

export function clampCalendarDate(date: Date, min: Date, max: Date): Date {
    return new Date(Math.max(min.getTime(), Math.min(max.getTime(), date.getTime())));
}

export function calendarMonthDays(month: Date): (Date | undefined)[] {
    const start = calendarMonthStart(month);
    const first = addCalendarDays(start, -start.getUTCDay());
    return Array.from({ length: 42 }, (_, index) => {
        const date = addCalendarDays(first, index);
        return date.getUTCFullYear() < 1 || date.getUTCFullYear() > 9999 ? undefined : date;
    });
}
