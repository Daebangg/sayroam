import { useId, type InputHTMLAttributes } from "react";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
    label: string;
    hint?: string;
    error?: string;
};

export function TextField({
    label,
    id,
    hint,
    error,
    className = "",
    "aria-describedby": describedBy,
    ...props
}: TextFieldProps) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    const message = error || hint;
    const messageId = `${inputId}-message`;
    return (
        <div className="ui-field">
            <label htmlFor={inputId}>{label}</label>
            <input
                id={inputId}
                className={`ui-input ${className}`}
                {...props}
                aria-invalid={error ? true : props["aria-invalid"]}
                aria-describedby={
                    [describedBy, message ? messageId : undefined].filter(Boolean).join(" ") ||
                    undefined
                }
            />
            {message && (
                <span
                    id={messageId}
                    className={`ui-field__message ${error ? "ui-field__message--error" : ""}`}
                >
                    {message}
                </span>
            )}
        </div>
    );
}
