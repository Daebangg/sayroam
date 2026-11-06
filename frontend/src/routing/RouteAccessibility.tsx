import { useEffect, useRef } from "react";
import { useLocation, useNavigation } from "react-router";
import { useTranslation } from "react-i18next";

export function RouteAccessibility() {
    const { key, pathname } = useLocation();
    const { state } = useNavigation();
    const { i18n, t } = useTranslation();
    const announcement = useRef<HTMLSpanElement>(null);
    const focusedPath = useRef<string | null>(null);
    useEffect(() => {
        if (state !== "idle" || focusedPath.current === pathname) return;
        focusedPath.current = pathname;
        if (document.querySelector("dialog[open]")) return;
        const heading = document.querySelector<HTMLElement>("main h1");
        heading?.setAttribute("tabindex", "-1");
        heading?.focus({ preventScroll: true });
    }, [pathname, state]);
    useEffect(() => {
        if (state !== "idle") return;
        const title =
            document.querySelector<HTMLElement>("main h1")?.innerText.replace(/\s+/g, " ").trim() ??
            t("app.name");
        document.title = `${title} · ${t("app.name")}`;
        if (announcement.current) announcement.current.textContent = title;
    }, [key, i18n.resolvedLanguage, t, state]);
    return <span className="visually-hidden" role="status" aria-live="polite" ref={announcement} />;
}
