import { Link, type LinkProps } from "react-router";

type ButtonLinkProps = LinkProps & { variant?: "brand" | "outline" };

export function ButtonLink({
    variant = "brand",
    className = "",
    children,
    ...props
}: ButtonLinkProps) {
    return (
        <Link className={`ui-button ui-button--${variant} ${className}`} {...props}>
            <span className="ui-button__label">{children}</span>
        </Link>
    );
}
