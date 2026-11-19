import { useId } from "react";
import { useTranslation } from "react-i18next";
import { NumberStepper } from "./NumberStepper";

type RangeSelectorProps = {
    label: string;
    lowerLabel: string;
    upperLabel: string;
    lowerValueLabel: string;
    upperValueLabel: string;
    min: number;
    max: number;
    value: readonly [number, number];
    disabled?: boolean;
    ticks?: readonly number[];
    onChange: (value: [number, number]) => void;
};

export function RangeSelector({
    label,
    lowerLabel,
    upperLabel,
    lowerValueLabel,
    upperValueLabel,
    min,
    max,
    value,
    disabled = false,
    ticks = [min, max],
    onChange,
}: RangeSelectorProps) {
    const { t } = useTranslation();
    const id = useId();
    const lower = Math.max(min, Math.min(value[0], max));
    const upper = Math.max(lower, Math.min(value[1], max));
    const span = max - min || 1;
    const start = ((lower - min) / span) * 100;
    const width = ((upper - lower) / span) * 100;
    return (
        <div
            className="ui-range-selector ui-menu-surface"
            role="group"
            aria-label={label}
            aria-disabled={disabled || undefined}
        >
            <div className="ui-calendar__header ui-range-selector__summary" aria-live="polite">
                <NumberStepper
                    label={lowerLabel}
                    min={min}
                    max={upper}
                    value={lower}
                    disabled={disabled}
                    onChange={(next) => onChange([next, upper])}
                />
                <span className="ui-range-selector__separator" aria-hidden="true">
                    {t("common.rangeSeparator")}
                </span>
                <NumberStepper
                    label={upperLabel}
                    min={lower}
                    max={max}
                    value={upper}
                    disabled={disabled}
                    onChange={(next) => onChange([lower, next])}
                />
            </div>
            <div
                className="ui-range-selector__track"
                data-upper-priority={lower === min || undefined}
            >
                <svg viewBox="0 0 100 8" preserveAspectRatio="none" aria-hidden="true">
                    <rect
                        className="ui-range-selector__base"
                        width="100"
                        height="8"
                        rx="0.6"
                        ry="4"
                    />
                    <rect
                        className="ui-range-selector__fill"
                        x={start}
                        width={width}
                        height="8"
                        rx="0.6"
                        ry="4"
                    />
                </svg>
                <input
                    id={`${id}-lower`}
                    type="range"
                    aria-label={lowerLabel}
                    aria-valuetext={lowerValueLabel}
                    min={min}
                    max={max}
                    value={lower}
                    disabled={disabled}
                    onChange={(event) =>
                        onChange([Math.min(Number(event.target.value), upper), upper])
                    }
                />
                <input
                    id={`${id}-upper`}
                    type="range"
                    aria-label={upperLabel}
                    aria-valuetext={upperValueLabel}
                    min={min}
                    max={max}
                    value={upper}
                    disabled={disabled}
                    onChange={(event) =>
                        onChange([lower, Math.max(lower, Number(event.target.value))])
                    }
                />
            </div>
            <div className="ui-range-selector__ticks" aria-hidden="true">
                {ticks
                    .filter((tick) => tick >= min && tick <= max)
                    .map((tick) => (
                        <span
                            key={tick}
                            className="ui-range-selector__tick"
                            data-selected={
                                (tick >= lower && tick <= upper && !disabled) || undefined
                            }
                            data-edge={tick === min ? "start" : tick === max ? "end" : undefined}
                            ref={(element) => {
                                element?.style.setProperty(
                                    "--tick-position",
                                    `${((tick - min) / span) * 100}%`,
                                );
                            }}
                        >
                            {tick}
                        </span>
                    ))}
            </div>
        </div>
    );
}
