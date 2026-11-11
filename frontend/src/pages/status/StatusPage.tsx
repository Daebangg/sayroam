import { useEffect } from "react";
import { isRouteErrorResponse, useRouteError } from "react-router";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink, PageState, type PageStateKind } from "@/components/ui";

type StatusPageProps = { kind: PageStateKind; reloadHome?: boolean; onRetry?: () => void };

export function StatusPage({ kind, reloadHome = false, onRetry }: StatusPageProps) {
    const { t } = useTranslation();
    useEffect(() => {
        if (kind === "loading") return;
        const heading = document.querySelector<HTMLElement>("main h1");
        heading?.setAttribute("tabindex", "-1");
        heading?.focus({ preventScroll: true });
    }, [kind]);
    useEffect(() => {
        if (kind === "loading") return;
        document.title = `${t(`pageState.${kind}.title`)} · ${t("app.name")}`;
    }, [kind, t]);
    return (
        <main id="main-content" tabIndex={-1} className="status-page status-page--illustrated">
            <PageState
                kind={kind}
                title={t(`pageState.${kind}.title`)}
                description={t(`pageState.${kind}.description`)}
                actions={
                    kind !== "loading" ? (
                        <>
                            <ButtonLink to="/" reloadDocument={reloadHome}>
                                {t("pageState.home")}
                            </ButtonLink>
                            {(kind === "error" || kind === "serverError") && (
                                <Button
                                    variant="outline"
                                    onClick={onRetry ?? (() => window.location.reload())}
                                >
                                    {t("pageState.retry")}
                                </Button>
                            )}
                        </>
                    ) : undefined
                }
            />
        </main>
    );
}

export function NotFoundPage() {
    return <StatusPage kind="notFound" />;
}
export function LoadingPage() {
    return <StatusPage kind="loading" />;
}
export function RouteErrorPage() {
    const error: unknown = useRouteError();
    return (
        <StatusPage
            kind={
                isRouteErrorResponse(error)
                    ? error.status === 404
                        ? "notFound"
                        : error.status >= 500
                          ? "serverError"
                          : "error"
                    : "error"
            }
        />
    );
}
export function RootErrorPage() {
    return (
        <div className="app-theme">
            <StatusPage kind="error" reloadHome />
        </div>
    );
}

export function RootLoadingPage() {
    return (
        <div className="app-theme">
            <LoadingPage />
        </div>
    );
}
