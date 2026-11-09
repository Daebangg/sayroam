import assert from "node:assert/strict";
import test from "node:test";
import {
    addCalendarDays,
    addCalendarMonths,
    calendarMonthDays,
    clampCalendarDate,
    formatCalendarDate,
    parseCalendarDate,
} from "../src/components/ui/inputs/calendar.ts";

function date(value: string): Date {
    const parsed = parseCalendarDate(value);
    assert.ok(parsed, `Expected a valid calendar date: ${value}`);
    return parsed;
}

test("ISO dates reject rolled-over days and unsupported years", () => {
    for (const invalid of [
        "2025-02-29",
        "2024-04-31",
        "0000-01-01",
        "10000-01-01",
        "2026-1-01",
        "",
        "2026-13-01",
    ])
        assert.equal(parseCalendarDate(invalid), undefined, invalid);
    for (const valid of ["0001-01-01", "0099-12-31", "2024-02-29", "9999-12-31"])
        assert.equal(formatCalendarDate(date(valid)), valid);
});

test("month navigation clamps the day at leap and ordinary February boundaries", () => {
    assert.equal(formatCalendarDate(addCalendarMonths(date("2024-01-31"), 1)), "2024-02-29");
    assert.equal(formatCalendarDate(addCalendarMonths(date("2025-01-31"), 1)), "2025-02-28");
    assert.equal(formatCalendarDate(addCalendarMonths(date("2024-02-29"), 12)), "2025-02-28");
    assert.equal(formatCalendarDate(addCalendarMonths(date("2026-01-31"), -1)), "2025-12-31");
});

test("day navigation uses calendar days across daylight saving and year boundaries", () => {
    assert.equal(formatCalendarDate(addCalendarDays(date("2026-03-08"), 1)), "2026-03-09");
    assert.equal(formatCalendarDate(addCalendarDays(date("2026-11-01"), -1)), "2026-10-31");
    assert.equal(formatCalendarDate(addCalendarDays(date("2026-12-31"), 1)), "2027-01-01");
});

test("keyboard movement stays inside inclusive minimum and maximum dates", () => {
    const min = date("2026-11-09");
    const max = date("2026-11-12");
    assert.equal(
        formatCalendarDate(clampCalendarDate(addCalendarDays(min, -7), min, max)),
        "2026-11-09",
    );
    assert.equal(
        formatCalendarDate(clampCalendarDate(addCalendarDays(max, 7), min, max)),
        "2026-11-12",
    );
    assert.equal(formatCalendarDate(clampCalendarDate(date("2026-11-10"), min, max)), "2026-11-10");
});

test("month grids always use six Sunday-first rows and contain every day of the month", () => {
    for (const value of ["2024-02-01", "2026-02-01", "2026-05-01", "2026-11-01"]) {
        const month = date(value);
        const days = calendarMonthDays(month);
        assert.equal(days.length, 42);
        assert.equal(days[0]?.getUTCDay(), 0);
        const matching = days.filter((day) => day?.getUTCMonth() === month.getUTCMonth());
        assert.equal(matching[0]?.getUTCDate(), 1);
        assert.ok(matching.length >= 28 && matching.length <= 31);
    }
});

test("supported year edges produce empty cells instead of dates outside the contract", () => {
    const first = calendarMonthDays(date("0001-01-01"));
    const last = calendarMonthDays(date("9999-12-01"));
    assert.ok(first.includes(undefined));
    assert.ok(last.includes(undefined));
    assert.ok(first.every((day) => !day || day.getUTCFullYear() >= 1));
    assert.ok(last.every((day) => !day || day.getUTCFullYear() <= 9999));
});
