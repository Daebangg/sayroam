import { Link, type LinkProps } from "react-router";

type ButtonLinkProps = LinkProps & {
    variant?: "primary" | "brand" | "highlight" | "edit" | "soft" | "outline" | "ghost" | "plain";
};

export function ButtonLink({
    variant = "primary",
    className = "",
    children,
    ...props
}: ButtonLinkProps) {
    return (
        <Link
            className={
                variant === "plain"
                    ? `ui-button-plain ${className}`
                    : `ui-button ui-button--${variant} ${className}`
            }
            {...props}
        >
            {variant === "plain" ? children : <span className="ui-button__label">{children}</span>}
        </Link>
    );
}
