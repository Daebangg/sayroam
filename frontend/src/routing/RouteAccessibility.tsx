import { useEffect, useRef } from "react";
import { useLocation, useNavigation } from "react-router";
import { useTranslation } from "react-i18next";

export function RouteAccessibility({ contentKey }: { contentKey?: string | undefined }) {
    const { key, pathname } = useLocation();
    const { state } = useNavigation();
    const { i18n, t } = useTranslation();
    const announcement = useRef<HTMLSpanElement>(null);
    const focusedPath = useRef<string | null>(null);
    useEffect(() => {
        const targetKey = pathname + (contentKey ?? "");
        if (state !== "idle" || focusedPath.current === targetKey) return;
        focusedPath.current = targetKey;
        if (document.querySelector("dialog[open]")) return;
        const heading = document.querySelector<HTMLElement>("main h1");
        heading?.setAttribute("tabindex", "-1");
        heading?.focus({ preventScroll: true });
    }, [pathname, state, contentKey]);
    useEffect(() => {
        if (state !== "idle") return;
        const title =
            document.querySelector<HTMLElement>("main h1")?.innerText.replace(/\s+/g, " ").trim() ??
            t("app.name");
        document.title = `${title} · ${t("app.name")}`;
        if (announcement.current) announcement.current.textContent = title;
    }, [key, i18n.resolvedLanguage, t, state, contentKey]);
    return <span className="visually-hidden" role="status" aria-live="polite" ref={announcement} />;
}
