import { useId, type InputHTMLAttributes } from "react";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & { label: string };

export function TextField({ label, id, className = "", ...props }: TextFieldProps) {
    const generatedId = useId();
    return (
        <label className="ui-field" htmlFor={id ?? generatedId}>
            <span>{label}</span>
            <input id={id ?? generatedId} className={`ui-input ${className}`} {...props} />
        </label>
    );
}
