import { useId, useRef } from "react";
import { useTranslation } from "react-i18next";

type VerificationCodeProps = {
    label: string;
    value: string;
    onChange: (value: string) => void;
    disabled?: boolean;
    error?: string;
    name?: string;
};

export function VerificationCode({
    label,
    value,
    onChange,
    disabled = false,
    error,
    name = "code",
}: VerificationCodeProps) {
    const { t } = useTranslation();
    const id = useId();
    const inputs = useRef<(HTMLInputElement | null)[]>([]);
    const focus = (index: number): void => {
        inputs.current[index]?.focus();
        inputs.current[index]?.select();
    };
    function insert(text: string, position: number): void {
        const digits = text.replace(/[^0-9]/g, "").slice(0, 6);
        if (!digits) return;
        const start = digits.length === 6 ? 0 : Math.min(position, value.length);
        const next = (value.slice(0, start) + digits + value.slice(start + digits.length)).slice(
            0,
            6,
        );
        onChange(next);
        focus(Math.min(start + digits.length, 5));
    }
    return (
        <div
            className="ui-field ui-verification-code"
            role="group"
            aria-labelledby={`${id}-label`}
            aria-describedby={error ? `${id}-error` : undefined}
        >
            <span className="ui-verification-code__label" id={`${id}-label`}>
                {label}
            </span>
            <input type="hidden" name={name} value={value} />
            <div className="ui-verification-code__digits">
                {Array.from({ length: 6 }, (_, index) => (
                    <input
                        key={index}
                        ref={(element) => {
                            inputs.current[index] = element;
                        }}
                        type="text"
                        inputMode="numeric"
                        autoComplete={index === 0 ? "one-time-code" : "off"}
                        value={value[index] ?? ""}
                        disabled={disabled}
                        aria-label={t("auth.codeDigit", { position: index + 1 })}
                        aria-invalid={error ? true : undefined}
                        aria-describedby={error ? `${id}-error` : undefined}
                        className="ui-verification-code__digit"
                        onFocus={(event) => event.currentTarget.select()}
                        onChange={(event) => {
                            if (event.target.value === "")
                                onChange(value.slice(0, index) + value.slice(index + 1));
                            else insert(event.target.value, index);
                        }}
                        onPaste={(event) => {
                            event.preventDefault();
                            insert(event.clipboardData.getData("text"), index);
                        }}
                        onKeyDown={(event) => {
                            if (event.key === "Backspace") {
                                event.preventDefault();
                                const target = value[index] ? index : Math.max(index - 1, 0);
                                onChange(value.slice(0, target) + value.slice(target + 1));
                                focus(target);
                            } else if (event.key === "ArrowLeft" || event.key === "ArrowRight") {
                                event.preventDefault();
                                focus(
                                    Math.min(
                                        5,
                                        Math.max(0, index + (event.key === "ArrowLeft" ? -1 : 1)),
                                    ),
                                );
                            } else if (event.key === "Home" || event.key === "End") {
                                event.preventDefault();
                                focus(event.key === "Home" ? 0 : 5);
                            }
                        }}
                    />
                ))}
            </div>
            {error && (
                <span id={`${id}-error`} className="ui-field__message ui-field__message--error">
                    {error}
                </span>
            )}
        </div>
    );
}
