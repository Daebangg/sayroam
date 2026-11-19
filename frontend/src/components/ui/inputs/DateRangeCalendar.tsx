import { useId, useRef, useState, type KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";
import {
    addCalendarDays,
    addCalendarMonths,
    calendarMonthDays,
    calendarMonthStart,
    calendarToday,
    formatCalendarDate,
    parseCalendarDate,
} from "./calendar";

type DateRangeCalendarProps = {
    label: string;
    startDate: string;
    endDate: string;
    onChange: (startDate: string, endDate: string) => void;
    disabled?: boolean;
};

export function DateRangeCalendar({
    label,
    startDate,
    endDate,
    onChange,
    disabled = false,
}: DateRangeCalendarProps) {
    const { t, i18n } = useTranslation();
    const locale = i18n.resolvedLanguage === "en" ? "en-US" : "ko-KR";
    const today = calendarToday();
    const todayIso = formatCalendarDate(today);
    const initial = parseCalendarDate(startDate) ?? today;
    const [month, setMonth] = useState(() => calendarMonthStart(initial < today ? today : initial));
    const [cursor, setCursor] = useState(() =>
        formatCalendarDate(initial < today ? today : initial),
    );
    const root = useRef<HTMLDivElement>(null);
    const id = useId();
    const monthFormat = new Intl.DateTimeFormat(locale, {
        year: "numeric",
        month: "long",
        timeZone: "UTC",
    });
    const dayFormat = new Intl.DateTimeFormat(locale, { dateStyle: "full", timeZone: "UTC" });
    const shortFormat = new Intl.DateTimeFormat(locale, {
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    });
    const weekdayFormat = new Intl.DateTimeFormat(locale, { weekday: "narrow", timeZone: "UTC" });
    const numberFormat = new Intl.NumberFormat(locale);
    function choose(value: string): void {
        if (!startDate || endDate || value < startDate) onChange(value, "");
        else onChange(startDate, value);
        setCursor(value);
    }
    function navigate(delta: number): void {
        const next = addCalendarMonths(month, delta);
        if (next.getUTCFullYear() < 1 || next.getUTCFullYear() > 9999) return;
        setMonth(next);
        setCursor(formatCalendarDate(next < today ? today : next));
    }
    function move(event: KeyboardEvent<HTMLButtonElement>, day: Date): void {
        let next: Date;
        switch (event.key) {
            case "ArrowLeft":
                next = addCalendarDays(day, -1);
                break;
            case "ArrowRight":
                next = addCalendarDays(day, 1);
                break;
            case "ArrowUp":
                next = addCalendarDays(day, -7);
                break;
            case "ArrowDown":
                next = addCalendarDays(day, 7);
                break;
            case "Home":
                next = addCalendarDays(day, -day.getUTCDay());
                break;
            case "End":
                next = addCalendarDays(day, 6 - day.getUTCDay());
                break;
            case "PageUp":
                next = addCalendarMonths(day, event.shiftKey ? -12 : -1);
                break;
            case "PageDown":
                next = addCalendarMonths(day, event.shiftKey ? 12 : 1);
                break;
            default:
                return;
        }
        event.preventDefault();
        if (next < today) next = today;
        if (next.getUTCFullYear() > 9999) return;
        const iso = formatCalendarDate(next);
        if (next < month || next >= addCalendarMonths(month, 2)) setMonth(calendarMonthStart(next));
        setCursor(iso);
        requestAnimationFrame(() =>
            root.current
                ?.querySelector<HTMLButtonElement>(`[data-range-date="${iso}"]`)
                ?.focus({ preventScroll: true }),
        );
    }
    return (
        <div ref={root} className="ui-range-calendar" role="group" aria-label={label}>
            <div className="ui-range-calendar__months">
                {[month, addCalendarMonths(month, 1)].map((visibleMonth, index) => {
                    const days = calendarMonthDays(visibleMonth);
                    const monthId = `${id}-month-${index}`;
                    return (
                        <section
                            key={index}
                            aria-labelledby={monthId}
                            className="ui-calendar ui-calendar--embedded"
                        >
                            <div className="ui-calendar__header">
                                <span
                                    className="ui-calendar__month"
                                    id={monthId}
                                    aria-live="polite"
                                >
                                    {monthFormat.format(visibleMonth)}
                                </span>
                                <div className="ui-calendar__navigation">
                                    {index === 0 && (
                                        <Button
                                            variant="plain"
                                            className="ui-calendar__nav ui-calendar__nav--previous"
                                            aria-label={t("controls.datePicker.previousMonth")}
                                            disabled={
                                                disabled || month <= calendarMonthStart(today)
                                            }
                                            onClick={() => navigate(-1)}
                                        >
                                            <Icon name="chevron" variant="outline" />
                                        </Button>
                                    )}
                                    {index === 1 && (
                                        <Button
                                            variant="plain"
                                            className="ui-calendar__nav ui-calendar__nav--next"
                                            aria-label={t("controls.datePicker.nextMonth")}
                                            disabled={
                                                disabled ||
                                                (month.getUTCFullYear() === 9999 &&
                                                    month.getUTCMonth() >= 10)
                                            }
                                            onClick={() => navigate(1)}
                                        >
                                            <Icon name="chevron" variant="outline" />
                                        </Button>
                                    )}
                                </div>
                            </div>
                            <table
                                className="ui-calendar__grid"
                                role="grid"
                                aria-labelledby={monthId}
                            >
                                <thead>
                                    <tr>
                                        {days.slice(7, 14).map((day, weekday) => (
                                            <th key={weekday} scope="col">
                                                {day && (
                                                    <abbr
                                                        title={new Intl.DateTimeFormat(locale, {
                                                            weekday: "long",
                                                            timeZone: "UTC",
                                                        }).format(day)}
                                                    >
                                                        {weekdayFormat.format(day)}
                                                    </abbr>
                                                )}
                                            </th>
                                        ))}
                                    </tr>
                                </thead>
                                <tbody>
                                    {Array.from({ length: 6 }, (_, week) => (
                                        <tr key={week}>
                                            {days
                                                .slice(week * 7, week * 7 + 7)
                                                .map((day, weekday) => {
                                                    const inMonth =
                                                        day &&
                                                        day.getUTCMonth() ===
                                                            visibleMonth.getUTCMonth();
                                                    const iso = day ? formatCalendarDate(day) : "";
                                                    const endpoint =
                                                        iso === startDate || iso === endDate;
                                                    const inRange = Boolean(
                                                        startDate &&
                                                        endDate &&
                                                        iso > startDate &&
                                                        iso < endDate,
                                                    );
                                                    return (
                                                        <td
                                                            key={weekday}
                                                            aria-selected={
                                                                inMonth
                                                                    ? endpoint || inRange
                                                                    : undefined
                                                            }
                                                            className={
                                                                inMonth &&
                                                                startDate &&
                                                                endDate &&
                                                                startDate !== endDate
                                                                    ? iso === startDate
                                                                        ? "ui-range-calendar__start"
                                                                        : iso === endDate
                                                                          ? "ui-range-calendar__end"
                                                                          : inRange
                                                                            ? "ui-range-calendar__between"
                                                                            : undefined
                                                                    : undefined
                                                            }
                                                        >
                                                            {inMonth && (
                                                                <Button
                                                                    variant="plain"
                                                                    className={`ui-calendar__day${endpoint ? " ui-calendar__day--selected" : ""}${iso === todayIso ? " ui-calendar__day--today" : ""}`}
                                                                    data-range-date={iso}
                                                                    aria-label={dayFormat.format(
                                                                        day,
                                                                    )}
                                                                    aria-current={
                                                                        iso === todayIso
                                                                            ? "date"
                                                                            : undefined
                                                                    }
                                                                    tabIndex={
                                                                        iso === cursor ? 0 : -1
                                                                    }
                                                                    disabled={
                                                                        disabled || day < today
                                                                    }
                                                                    onKeyDown={(event) =>
                                                                        move(event, day)
                                                                    }
                                                                    onClick={() => choose(iso)}
                                                                >
                                                                    {numberFormat.format(
                                                                        day.getUTCDate(),
                                                                    )}
                                                                </Button>
                                                            )}
                                                        </td>
                                                    );
                                                })}
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </section>
                    );
                })}
            </div>
            <div className="ui-range-calendar__summary" aria-live="polite">
                <span className={`ui-range-calendar__selection${startDate ? "" : " is-empty"}`}>
                    {startDate ? (
                        <>
                            {t("controls.dateRange.start")}{" "}
                            <strong>{shortFormat.format(parseCalendarDate(startDate))}</strong>
                        </>
                    ) : (
                        t("controls.dateRange.selectStart")
                    )}
                </span>
                <Icon name="arrow" variant="outline" />
                <span className={`ui-range-calendar__selection${endDate ? "" : " is-empty"}`}>
                    {endDate ? (
                        <>
                            {t("controls.dateRange.end")}{" "}
                            <strong>{shortFormat.format(parseCalendarDate(endDate))}</strong>
                        </>
                    ) : (
                        t("controls.dateRange.selectEnd")
                    )}
                </span>
            </div>
        </div>
    );
}
