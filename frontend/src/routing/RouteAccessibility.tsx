import { useEffect, useRef } from "react";
import { useLocation } from "react-router";
import { useTranslation } from "react-i18next";

export function RouteAccessibility() {
    const { key } = useLocation();
    const { i18n, t } = useTranslation();
    const announcement = useRef<HTMLSpanElement>(null);
    useEffect(() => {
        if (document.querySelector("dialog[open]")) return;
        const heading = document.querySelector<HTMLElement>("main h1");
        heading?.setAttribute("tabindex", "-1");
        heading?.focus({ preventScroll: true });
    }, [key]);
    useEffect(() => {
        const title =
            document.querySelector<HTMLElement>("main h1")?.innerText.replace(/\s+/g, " ").trim() ??
            t("app.name");
        document.title = `${title} · ${t("app.name")}`;
        if (announcement.current) announcement.current.textContent = title;
    }, [key, i18n.resolvedLanguage, t]);
    return <span className="visually-hidden" role="status" aria-live="polite" ref={announcement} />;
}
