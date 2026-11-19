import { useId, type TextareaHTMLAttributes } from "react";

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
    label: string;
    hint?: string;
};

export function TextArea({
    label,
    hint,
    id,
    className = "",
    "aria-describedby": describedBy,
    ...props
}: TextAreaProps) {
    const generatedId = useId();
    const inputId = id ?? generatedId;
    return (
        <div className="ui-field">
            <label htmlFor={inputId}>{label}</label>
            <textarea
                id={inputId}
                className={`ui-input ui-textarea ${className}`}
                aria-describedby={
                    [describedBy, hint ? `${inputId}-hint` : undefined].filter(Boolean).join(" ") ||
                    undefined
                }
                {...props}
            />
            {hint && (
                <span id={`${inputId}-hint`} className="ui-field__message">
                    {hint}
                </span>
            )}
        </div>
    );
}
