import { cloneElement, useId, useLayoutEffect, useRef, useState, type ReactElement } from "react";
import { createPortal } from "react-dom";
import { KeyboardShortcut } from "../navigation/KeyboardShortcut";

type TooltipProps = {
    content: string;
    shortcut?: readonly string[];
    className?: string;
    children: ReactElement<{ "aria-describedby"?: string | undefined }>;
};

export function Tooltip({ content, shortcut, children, className = "" }: TooltipProps) {
    const [open, setOpen] = useState(false);
    const [portal, setPortal] = useState<HTMLElement>(() => document.body);
    const trigger = useRef<HTMLSpanElement>(null);
    const panel = useRef<HTMLSpanElement>(null);
    const keyboard = useRef(false);
    const id = useId();

    useLayoutEffect(() => {
        if (!open) return;
        const update = () => {
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
    }, [open, content]);

    function showOverlay(): void {
        setPortal(trigger.current?.closest("dialog") ?? document.body);
        setOpen(true);
    }
    return (
        <span
            ref={trigger}
            className={`ui-tooltip-trigger ${className}`}
            onMouseEnter={() => showOverlay()}
            onMouseLeave={() => {
                if (!keyboard.current) setOpen(false);
            }}
            onPointerDownCapture={() => {
                keyboard.current = false;
                setOpen(false);
            }}
            onClickCapture={() => setOpen(false)}
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
            {open &&
                createPortal(
                    <span ref={panel} id={id} role="tooltip" className="ui-tooltip-content">
                        <span>{content}</span>
                        {shortcut && <KeyboardShortcut keys={shortcut} />}
                    </span>,
                    portal,
                )}
        </span>
    );
}
