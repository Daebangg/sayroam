import type { ButtonHTMLAttributes, Ref } from "react";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    ref?: Ref<HTMLButtonElement>;
} & (
        | { variant: "plain"; loading?: never }
        | {
              variant?: "primary" | "brand" | "soft" | "outline" | "ghost" | "danger";
              loading?: boolean;
          }
    );

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
            className={
                variant === "plain"
                    ? className
                    : `ui-button ui-button--${variant} ${loading ? "ui-button--loading" : ""} ${className}`
            }
            disabled={disabled || loading}
            aria-busy={loading || undefined}
            {...props}
        >
            {variant === "plain" ? children : <span className="ui-button__label">{children}</span>}
            {loading && <span className="ui-spinner" aria-hidden="true" />}
        </button>
    );
}
