import type { HTMLAttributes } from "react";

export function Card({ className = "", ...props }: HTMLAttributes<HTMLElement>) {
    return <article className={`ui-card ${className}`} {...props} />;
}
