import { useId, type ReactNode } from "react";
import { Spinner } from "./Spinner";

export type PageStateKind = "notFound" | "error" | "serverError" | "loading";
type PageStateProps = {
    kind: PageStateKind;
    title: string;
    description: string;
    headingAs?: "h1" | "h3";
    actions?: ReactNode;
};

export function PageState({
    kind,
    title,
    description,
    headingAs: Heading = "h1",
    actions,
}: PageStateProps) {
    const id = useId();
    return (
        <section className={`ui-page-state ui-page-state--${kind}`} aria-labelledby={id}>
            {kind === "loading" ? (
                <Spinner label={title} labelHidden />
            ) : (
                <div className="ui-page-state__visual" aria-hidden="true">
                    <span>
                        {kind === "notFound" ? "404" : kind === "serverError" ? "500" : "!"}
                    </span>
                    <img src="/brand/sayroam-mark.svg" alt="" />
                </div>
            )}
            <Heading id={id}>{title}</Heading>
            <p>{description}</p>
            {actions && <div className="ui-page-state__actions">{actions}</div>}
        </section>
    );
}
