import { useEffect, useEffectEvent, useRef, type ReactNode } from "react";
import { createPortal } from "react-dom";

type DetailsSidebarProps = {
    id: string;
    titleId: string;
    closing: boolean;
    onClose: () => void;
    onExited: () => void;
    children: ReactNode;
};

export function DetailsSidebar({
    id,
    titleId,
    closing,
    onClose,
    onExited,
    children,
}: DetailsSidebarProps) {
    const panel = useRef<HTMLElement>(null);
    const close = useEffectEvent(onClose);
    useEffect(() => {
        const opener = document.activeElement;
        const media = window.matchMedia("(max-width: 700px)");
        const page = document.getElementById("main-content");
        const previousOverflow = document.body.style.overflow;
        function update(): void {
            if (page) page.inert = media.matches;
            document.body.style.overflow = media.matches ? "hidden" : previousOverflow;
        }
        update();
        panel.current?.focus({ preventScroll: true });
        function keydown(event: KeyboardEvent): void {
            if (event.defaultPrevented || document.querySelector("dialog[open], :popover-open"))
                return;
            if (
                event.target instanceof Element &&
                event.target.closest(".site-header, .chat-sidebar")
            )
                return;
            if (event.key === "Escape" && !event.isComposing) {
                event.preventDefault();
                close();
                return;
            }
        }
        function outside(event: PointerEvent): void {
            const target = event.target;
            if (
                !(target instanceof Element) ||
                panel.current?.contains(target) ||
                document.querySelector("dialog[open]")
            )
                return;
            if (id === "home-details-panel" && target.closest(".home-planner__rail")) return;
            // Let another recruitment card handle selection without closing its new panel.
            if (
                target.closest(
                    ".matching-registration__open, .matching-registration, .roam-match-card, .ui-dropdown__menu, .chat-sidebar, [aria-keyshortcuts='Alt+C']",
                )
            )
                return;
            close();
        }
        document.addEventListener("pointerdown", outside);
        media.addEventListener("change", update);
        document.addEventListener("keydown", keydown);
        return () => {
            document.removeEventListener("pointerdown", outside);
            media.removeEventListener("change", update);
            document.removeEventListener("keydown", keydown);
            if (page) page.inert = false;
            document.body.style.overflow = previousOverflow;
            if (opener instanceof HTMLElement && opener.isConnected)
                opener.focus({ preventScroll: true });
        };
    }, [id]);
    return createPortal(
        <aside
            ref={panel}
            id={id}
            tabIndex={-1}
            role="region"
            aria-labelledby={titleId}
            className={`edge-panel matching-details-panel${closing ? " matching-details-panel--closing" : ""}`}
            onAnimationEnd={(event) => {
                if (
                    closing &&
                    event.target === event.currentTarget &&
                    event.animationName === "matching-panel-exit"
                )
                    onExited();
            }}
        >
            {children}
        </aside>,
        document.querySelector(".app-shell") ?? document.body,
    );
}
