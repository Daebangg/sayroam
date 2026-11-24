import { cloneElement, useId, useLayoutEffect, useRef, useState, type ReactElement } from "react";
import { createPortal } from "react-dom";
import { KeyboardShortcut } from "../navigation/KeyboardShortcut";

type TooltipProps = {
    content: string;
    shortcut?: readonly string[];
    className?: string;
    touchHint?: boolean;
    placement?: "viewport" | "below-end";
    children: ReactElement<{ "aria-describedby"?: string | undefined }>;
};

export function Tooltip({
    content,
    shortcut,
    children,
    className = "",
    touchHint = false,
    placement = "viewport",
}: TooltipProps) {
    const [open, setOpen] = useState(false);
    const [portal, setPortal] = useState<HTMLElement>(() => document.body);
    const trigger = useRef<HTMLSpanElement>(null);
    const panel = useRef<HTMLSpanElement>(null);
    const keyboard = useRef(false);
    const touchStart = useRef<number | null>(null);
    const held = useRef(false);
    const id = useId();

    useLayoutEffect(() => {
        if (!open) return;
        const update = () => {
            if (placement === "below-end") return;
            const anchor = trigger.current?.getBoundingClientRect();
            const tip = panel.current;
            if (!anchor || !tip) return;
            const width = document.documentElement.clientWidth;
            const height = document.documentElement.clientHeight;
            const top =
                anchor.bottom + tip.offsetHeight + 8 < height
                    ? anchor.bottom + 8
                    : anchor.top - tip.offsetHeight - 8;
            tip.style.left = `${Math.max(8, Math.min(anchor.left + (anchor.width - tip.offsetWidth) / 2, width - tip.offsetWidth - 8))}px`;
            tip.style.top = `${Math.max(8, top)}px`;
            tip.style.visibility = anchor.bottom < 0 || anchor.top > height ? "hidden" : "visible";
        };
        const dismiss = () => {
            keyboard.current = false;
            setOpen(false);
        };
        const escape = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                event.stopPropagation();
                dismiss();
            }
        };
        update();
        window.addEventListener("resize", update);
        window.addEventListener("scroll", update, true);
        window.addEventListener("blur", dismiss);
        window.addEventListener("keydown", escape);
        return () => {
            window.removeEventListener("resize", update);
            window.removeEventListener("scroll", update, true);
            window.removeEventListener("blur", dismiss);
            window.removeEventListener("keydown", escape);
        };
    }, [open, content, placement]);

    function showOverlay(): void {
        setPortal(trigger.current?.closest("dialog") ?? document.body);
        setOpen(true);
    }
    const tip = (
        <span ref={panel} id={id} role="tooltip" className="ui-tooltip-content">
            <span>{content}</span>
            {shortcut && <KeyboardShortcut keys={shortcut} />}
        </span>
    );
    return (
        <span
            ref={trigger}
            className={`ui-tooltip-trigger ${placement === "below-end" ? "ui-tooltip-trigger--below-end" : ""} ${className}`}
            onPointerEnter={(event) => {
                if (event.pointerType !== "touch") showOverlay();
            }}
            onPointerLeave={() => {
                if (!keyboard.current) setOpen(false);
            }}
            onPointerDownCapture={(event) => {
                keyboard.current = false;
                held.current = false;
                if (touchHint && event.pointerType === "touch") {
                    touchStart.current = event.timeStamp;
                    showOverlay();
                    return;
                }
                touchStart.current = null;
                setOpen(false);
            }}
            onContextMenu={(event) => {
                if (touchStart.current !== null) event.preventDefault();
            }}
            onPointerUp={(event) => {
                if (touchStart.current === null) return;
                held.current = event.timeStamp - touchStart.current >= 450;
                touchStart.current = null;
                setOpen(false);
            }}
            onPointerCancel={() => {
                touchStart.current = null;
                setOpen(false);
            }}
            onClickCapture={(event) => {
                if (held.current && event.detail > 0) {
                    event.preventDefault();
                    event.stopPropagation();
                }
                held.current = false;
                setOpen(false);
            }}
            onFocusCapture={(event) => {
                keyboard.current = event.target.matches(":focus-visible");
                if (keyboard.current) showOverlay();
            }}
            onBlurCapture={() => {
                keyboard.current = false;
                setOpen(false);
            }}
        >
            {cloneElement(children, {
                "aria-describedby": open
                    ? [children.props["aria-describedby"], id].filter(Boolean).join(" ")
                    : children.props["aria-describedby"],
            })}
            {open && (placement === "below-end" ? tip : createPortal(tip, portal))}
        </span>
    );
}
