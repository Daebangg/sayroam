import type { ButtonHTMLAttributes, Ref } from "react";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    variant?: "primary" | "brand" | "soft" | "outline" | "ghost" | "danger";
    loading?: boolean;
    ref?: Ref<HTMLButtonElement>;
};

export function Button({
    variant = "primary",
    className = "",
    type = "button",
    loading = false,
    disabled,
    children,
    ...props
}: ButtonProps) {
    return (
        <button
            type={type}
            className={`ui-button ui-button--${variant} ${loading ? "ui-button--loading" : ""} ${className}`}
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            {...props}
        >
            <span className="ui-button__label">{children}</span>
            {loading && <span className="ui-spinner" aria-hidden="true" />}
        </button>
    );
}
