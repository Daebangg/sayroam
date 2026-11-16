import { useId } from "react";

type RangeFieldProps = {
    label: string;
    value: number;
    valueLabel: string;
    min: number;
    max: number;
    disabled?: boolean;
    onChange: (value: number) => void;
};

export function RangeField({
    label,
    value,
    valueLabel,
    min,
    max,
    disabled,
    onChange,
}: RangeFieldProps) {
    const id = useId();
    return (
        <div className="ui-range-field">
            <div className="ui-range-field__heading">
                <label htmlFor={id}>{label}</label>
                <output htmlFor={id}>{valueLabel}</output>
            </div>
            <input
                id={id}
                type="range"
                min={min}
                max={max}
                step={1}
                value={value}
                disabled={disabled}
                aria-valuetext={valueLabel}
                onChange={(event) => onChange(Number(event.target.value))}
            />
        </div>
    );
}
