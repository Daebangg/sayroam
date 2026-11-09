import { useId, useState, type KeyboardEvent } from "react";
import { useTranslation } from "react-i18next";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";

type NumberStepperProps = {
    label: string;
    name?: string;
    value: number;
    onChange: (value: number) => void;
    min?: number;
    max?: number;
    disabled?: boolean;
    className?: string;
};

export function NumberStepper({
    label,
    name,
    value,
    onChange,
    min = 1,
    max = 20,
    disabled = false,
    className = "",
}: NumberStepperProps) {
    const { t } = useTranslation();
    const id = useId();
    const [draft, setDraft] = useState<string | null>(null);
    const lowerBound = Number.isFinite(min) ? Math.ceil(min) : 1;
    const upperBound = Number.isFinite(max) ? Math.floor(max) : 20;
    const unavailable = disabled || lowerBound > upperBound;
    const clamp = (next: number) => Math.max(lowerBound, Math.min(upperBound, Math.trunc(next)));
    const current = clamp(Number.isFinite(value) ? value : lowerBound);

    function change(next: number): void {
        const clamped = clamp(next);
        setDraft(null);
        if (clamped !== value) onChange(clamped);
    }

    function handleKey(event: KeyboardEvent<HTMLInputElement>): void {
        const keyValues: Record<string, number> = {
            ArrowUp: current + 1,
            ArrowDown: current - 1,
            PageUp: current + 5,
            PageDown: current - 5,
            Home: lowerBound,
            End: upperBound,
        };
        const next = keyValues[event.key];
        if (next === undefined) return;
        event.preventDefault();
        change(next);
    }

    return (
        <div className={`ui-number-stepper ${className}`}>
            <label htmlFor={id} className="ui-number-stepper__label">
                {label}
            </label>
            <div className="ui-number-stepper__control" data-disabled={unavailable || undefined}>
                <Button
                    variant="plain"
                    className="ui-number-stepper__button"
                    aria-label={t("controls.numberStepper.decrease", { label })}
                    aria-controls={id}
                    disabled={unavailable || current <= lowerBound}
                    onPointerDown={(event) => event.preventDefault()}
                    onClick={() => change(current - 1)}
                >
                    <Icon name="minus" variant="outline" />
                </Button>
                <input
                    id={id}
                    className="ui-number-stepper__input"
                    type="text"
                    inputMode="numeric"
                    pattern="-?[0-9]*"
                    role="spinbutton"
                    autoComplete="off"
                    value={draft ?? String(current)}
                    aria-valuemin={lowerBound}
                    aria-valuemax={upperBound}
                    aria-valuenow={current}
                    disabled={unavailable}
                    onChange={(event) => {
                        const next = event.target.value;
                        if (!/^-?\d*$/.test(next)) return;
                        setDraft(next);
                        const number = Number(next);
                        if (
                            next &&
                            Number.isSafeInteger(number) &&
                            number >= lowerBound &&
                            number <= upperBound
                        )
                            onChange(number);
                    }}
                    onBlur={() => {
                        const number = Number(draft);
                        change(draft && Number.isSafeInteger(number) ? number : current);
                    }}
                    onKeyDown={handleKey}
                />
                <Button
                    variant="plain"
                    className="ui-number-stepper__button ui-number-stepper__button--increase"
                    aria-label={t("controls.numberStepper.increase", { label })}
                    aria-controls={id}
                    disabled={unavailable || current >= upperBound}
                    onPointerDown={(event) => event.preventDefault()}
                    onClick={() => change(current + 1)}
                >
                    <Icon name="plus" variant="outline" />
                </Button>
            </div>
            {name && <input type="hidden" name={name} value={current} disabled={unavailable} />}
        </div>
    );
}
