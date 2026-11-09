import { useEffect, useId, useLayoutEffect, useRef, useState, type KeyboardEvent } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { normalizeLanguage } from "../../../i18n/locale";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";
import {
    addCalendarDays,
    addCalendarMonths,
    calendarMonthDays,
    calendarMonthStart,
    calendarToday,
    clampCalendarDate,
    formatCalendarDate,
    parseCalendarDate,
} from "./calendar";

type DatePickerProps = {
    label: string;
    name?: string;
    value: string;
    onChange: (value: string) => void;
    min?: string;
    max?: string;
    disabled?: boolean;
    placeholder?: string;
    className?: string;
};

const firstDate = parseCalendarDate("0001-01-01")!;
const lastDate = parseCalendarDate("9999-12-31")!;

export function DatePicker({
    label,
    name,
    value,
    onChange,
    min,
    max,
    disabled = false,
    placeholder,
    className = "",
}: DatePickerProps) {
    const { t, i18n } = useTranslation();
    const locale = normalizeLanguage(i18n.resolvedLanguage) === "en" ? "en-US" : "ko-KR";
    const selected = parseCalendarDate(value);
    const lowerBound = parseCalendarDate(min) ?? firstDate;
    const upperBound = parseCalendarDate(max) ?? lastDate;
    const unavailable = disabled || lowerBound > upperBound;
    const [phase, setPhase] = useState<"closed" | "open" | "closing">("closed");
    const [activeDate, setActiveDate] = useState(() => selected ?? calendarToday());
    const [portal, setPortal] = useState<HTMLElement>(() => document.body);
    const root = useRef<HTMLDivElement>(null);
    const trigger = useRef<HTMLButtonElement>(null);
    const panel = useRef<HTMLDivElement>(null);
    const focusDay = useRef(false);
    const id = useId();
    const panelId = `${id}-calendar`;
    const monthId = `${id}-month`;
    const visible = phase !== "closed" && !unavailable;
    const open = phase === "open" && !unavailable;
    const month = calendarMonthStart(activeDate);
    const today = formatCalendarDate(calendarToday());
    const monthLabel = new Intl.DateTimeFormat(locale, {
        year: "numeric",
        month: "long",
        timeZone: "UTC",
    }).format(month);
    const dateFormatter = new Intl.DateTimeFormat(locale, {
        year: "numeric",
        month: "long",
        day: "numeric",
        weekday: "long",
        timeZone: "UTC",
    });
    const shortFormatter = new Intl.DateTimeFormat(locale, {
        year: "numeric",
        month: "short",
        day: "numeric",
        timeZone: "UTC",
    });
    const numberFormatter = new Intl.NumberFormat(locale);
    const weekdayFormatter = new Intl.DateTimeFormat(locale, {
        weekday: "narrow",
        timeZone: "UTC",
    });
    const fullWeekdayFormatter = new Intl.DateTimeFormat(locale, {
        weekday: "long",
        timeZone: "UTC",
    });
    const days = calendarMonthDays(month);
    const emptyLabel = placeholder ?? t("controls.datePicker.placeholder");

    useLayoutEffect(() => {
        if (!visible) return;
        const element = panel.current;
        if (!element) return;
        element.showPopover();
        // The calendar owns only viewport geometry; its appearance stays in app.css.
        const position = () => {
            const rect = trigger.current?.getBoundingClientRect();
            if (!rect || !element.isConnected) return;
            const viewport = document.documentElement;
            const edge = 12;
            const gap = 8;
            element.style.width = `${Math.min(328, viewport.clientWidth - edge * 2)}px`;
            const below = viewport.clientHeight - rect.bottom - edge - gap;
            const above = rect.top - edge - gap;
            const up = element.scrollHeight > below && above > below;
            element.style.maxHeight = `${Math.max(80, up ? above : below)}px`;
            element.style.left = `${Math.max(edge, Math.min(rect.left, viewport.clientWidth - element.offsetWidth - edge))}px`;
            element.style.top = `${Math.max(edge, Math.min(up ? rect.top - element.offsetHeight - gap : rect.bottom + gap, viewport.clientHeight - element.offsetHeight - edge))}px`;
            element.dataset.placement = up ? "top" : "bottom";
            element.style.visibility = "visible";
        };
        position();
        const resizeObserver = new ResizeObserver(position);
        if (trigger.current) resizeObserver.observe(trigger.current);
        window.addEventListener("resize", position);
        window.addEventListener("scroll", position, true);
        return () => {
            resizeObserver.disconnect();
            window.removeEventListener("resize", position);
            window.removeEventListener("scroll", position, true);
            if (element.matches(":popover-open")) element.hidePopover();
        };
    }, [visible]);

    useLayoutEffect(() => {
        if (!open || !focusDay.current) return;
        const element = panel.current;
        const day = element?.querySelector<HTMLButtonElement>(
            `[data-date="${formatCalendarDate(activeDate)}"]`,
        );
        day?.focus({ preventScroll: true });
        if (element && day && element.scrollHeight > element.clientHeight) {
            const container = element.getBoundingClientRect();
            const target = day.getBoundingClientRect();
            if (target.bottom > container.bottom - 8)
                element.scrollTop += target.bottom - container.bottom + 8;
            else if (target.top < container.top + 8)
                element.scrollTop -= container.top - target.top + 8;
        }
        focusDay.current = false;
    }, [activeDate, open]);

    useEffect(() => {
        if (!open) return;
        const outside = (event: PointerEvent) => {
            if (
                event.target instanceof Node &&
                !root.current?.contains(event.target) &&
                !panel.current?.contains(event.target)
            )
                setPhase("closing");
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [open]);

    useEffect(() => {
        if (phase !== "closing") return;
        const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        const timer = window.setTimeout(() => setPhase("closed"), reduced ? 0 : 160);
        return () => window.clearTimeout(timer);
    }, [phase]);

    function showCalendar(): void {
        if (unavailable) return;
        setPortal(root.current?.closest("dialog") ?? document.body);
        setActiveDate(clampCalendarDate(selected ?? calendarToday(), lowerBound, upperBound));
        focusDay.current = true;
        setPhase("open");
    }

    function closeCalendar(restoreFocus: boolean): void {
        setPhase("closing");
        if (restoreFocus) trigger.current?.focus({ preventScroll: true });
    }

    function selectDate(date: string): void {
        onChange(date);
        closeCalendar(true);
    }

    function moveDate(date: Date, focus = true): void {
        focusDay.current = focus;
        setActiveDate(clampCalendarDate(date, lowerBound, upperBound));
    }

    function handleDateKey(event: KeyboardEvent<HTMLButtonElement>): void {
        let next: Date;
        switch (event.key) {
            case "ArrowLeft":
                next = addCalendarDays(activeDate, -1);
                break;
            case "ArrowRight":
                next = addCalendarDays(activeDate, 1);
                break;
            case "ArrowUp":
                next = addCalendarDays(activeDate, -7);
                break;
            case "ArrowDown":
                next = addCalendarDays(activeDate, 7);
                break;
            case "Home":
                next = addCalendarDays(activeDate, -activeDate.getUTCDay());
                break;
            case "End":
                next = addCalendarDays(activeDate, 6 - activeDate.getUTCDay());
                break;
            case "PageUp":
                next = addCalendarMonths(activeDate, event.shiftKey ? -12 : -1);
                break;
            case "PageDown":
                next = addCalendarMonths(activeDate, event.shiftKey ? 12 : 1);
                break;
            default:
                return;
        }
        event.preventDefault();
        moveDate(next);
    }

    return (
        <div
            ref={root}
            className={`ui-date-picker ${className}`}
            onKeyDown={(event) => {
                if (event.key !== "Escape" || !open) return;
                event.preventDefault();
                event.stopPropagation();
                closeCalendar(true);
            }}
            onBlur={(event) => {
                const next = event.relatedTarget;
                if (
                    open &&
                    (!(next instanceof Node) ||
                        (!root.current?.contains(next) && !panel.current?.contains(next)))
                )
                    closeCalendar(false);
            }}
        >
            <label htmlFor={id} className="ui-date-picker__label">
                {label}
            </label>
            <Button
                variant="plain"
                ref={trigger}
                id={id}
                className="ui-date-picker__trigger"
                aria-label={`${label}, ${selected ? shortFormatter.format(selected) : emptyLabel}`}
                aria-haspopup="dialog"
                aria-expanded={open}
                aria-controls={visible ? panelId : undefined}
                disabled={unavailable}
                onClick={() => (open ? closeCalendar(false) : showCalendar())}
                onKeyDown={(event) => {
                    if (event.key !== "ArrowDown" && event.key !== "ArrowUp") return;
                    event.preventDefault();
                    showCalendar();
                }}
            >
                <Icon name="calendar" />
                <span
                    className={`ui-date-picker__value${selected ? "" : " ui-date-picker__value--empty"}`}
                >
                    {selected ? shortFormatter.format(selected) : emptyLabel}
                </span>
                <Icon name="chevron" variant="outline" className="ui-date-picker__chevron" />
            </Button>
            {name && (
                <input
                    type="hidden"
                    name={name}
                    value={selected ? formatCalendarDate(selected) : ""}
                    disabled={unavailable}
                />
            )}
            {visible &&
                createPortal(
                    <div
                        ref={panel}
                        id={panelId}
                        role="dialog"
                        aria-modal="false"
                        aria-label={label}
                        aria-hidden={!open || undefined}
                        inert={!open}
                        popover="manual"
                        className={`ui-calendar${phase === "closing" ? " ui-calendar--closing" : ""}`}
                    >
                        <div className="ui-calendar__header">
                            <span className="ui-calendar__month" id={monthId} aria-live="polite">
                                {monthLabel}
                            </span>
                            <div className="ui-calendar__navigation">
                                <Button
                                    variant="plain"
                                    className="ui-calendar__nav ui-calendar__nav--previous"
                                    aria-label={t("controls.datePicker.previousMonth")}
                                    disabled={month <= calendarMonthStart(lowerBound)}
                                    onClick={() =>
                                        moveDate(addCalendarMonths(activeDate, -1), false)
                                    }
                                >
                                    <Icon name="chevron" variant="outline" />
                                </Button>
                                <Button
                                    variant="plain"
                                    className="ui-calendar__nav ui-calendar__nav--next"
                                    aria-label={t("controls.datePicker.nextMonth")}
                                    disabled={month >= calendarMonthStart(upperBound)}
                                    onClick={() =>
                                        moveDate(addCalendarMonths(activeDate, 1), false)
                                    }
                                >
                                    <Icon name="chevron" variant="outline" />
                                </Button>
                            </div>
                        </div>
                        <table className="ui-calendar__grid" role="grid" aria-labelledby={monthId}>
                            <thead>
                                <tr>
                                    {days.slice(7, 14).map((day, index) => (
                                        <th key={index} scope="col">
                                            {day && (
                                                <abbr title={fullWeekdayFormatter.format(day)}>
                                                    {weekdayFormatter.format(day)}
                                                </abbr>
                                            )}
                                        </th>
                                    ))}
                                </tr>
                            </thead>
                            <tbody key={formatCalendarDate(month)}>
                                {Array.from({ length: 6 }, (_, week) => (
                                    <tr key={week}>
                                        {days.slice(week * 7, week * 7 + 7).map((day, weekday) => {
                                            const iso = day ? formatCalendarDate(day) : "";
                                            const isSelected = iso === value;
                                            const isToday = iso === today;
                                            const outside =
                                                day?.getUTCMonth() !== month.getUTCMonth();
                                            const blocked =
                                                !day || day < lowerBound || day > upperBound;
                                            return (
                                                <td key={weekday} aria-selected={isSelected}>
                                                    {day && (
                                                        <Button
                                                            variant="plain"
                                                            className={`ui-calendar__day${outside ? " ui-calendar__day--outside" : ""}${isSelected ? " ui-calendar__day--selected" : ""}${isToday ? " ui-calendar__day--today" : ""}`}
                                                            data-date={iso}
                                                            aria-label={dateFormatter.format(day)}
                                                            aria-current={
                                                                isToday ? "date" : undefined
                                                            }
                                                            tabIndex={
                                                                iso ===
                                                                formatCalendarDate(activeDate)
                                                                    ? 0
                                                                    : -1
                                                            }
                                                            disabled={blocked}
                                                            onKeyDown={handleDateKey}
                                                            onClick={() => selectDate(iso)}
                                                        >
                                                            {numberFormatter.format(
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
                        <div className="ui-calendar__footer">
                            <Button
                                variant="plain"
                                className="ui-calendar__clear"
                                disabled={!value}
                                onClick={() => selectDate("")}
                            >
                                {t("controls.datePicker.clear")}
                            </Button>
                            <Button
                                variant="plain"
                                className="ui-calendar__today"
                                disabled={
                                    calendarToday() < lowerBound || calendarToday() > upperBound
                                }
                                onClick={() => selectDate(today)}
                            >
                                {t("controls.datePicker.today")}
                            </Button>
                        </div>
                    </div>,
                    portal,
                )}
        </div>
    );
}
