import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { MenuItem } from "./MenuItem";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";

type DropdownOption = { value: string; label: string; disabled?: boolean };
type DropdownMenuProps = {
    label: string;
    value: string;
    options: readonly DropdownOption[];
    onChange: (value: string) => void;
    disabled?: boolean;
    overlayOwner?: string;
};

export function DropdownMenu({
    label,
    value,
    options,
    onChange,
    disabled,
    overlayOwner,
}: DropdownMenuProps) {
    const [open, setOpen] = useState(false);
    const [closing, setClosing] = useState(false);
    const [portal, setPortal] = useState<HTMLElement>(() => document.body);
    const root = useRef<HTMLDivElement>(null);
    const trigger = useRef<HTMLButtonElement>(null);
    const menu = useRef<HTMLDivElement>(null);
    const scrollArea = useRef<HTMLDivElement>(null);
    const id = useId();

    useLayoutEffect(() => {
        const element = menu.current;
        if (!open || !overlayOwner || !element) return;
        element.showPopover();
        return () => {
            if (element.matches(":popover-open")) element.hidePopover();
        };
    }, [open, overlayOwner]);

    useLayoutEffect(() => {
        if (!open || closing) return;
        const update = () => {
            const rect = trigger.current?.getBoundingClientRect();
            const element = menu.current;
            if (!rect || !element) return;
            const viewport = document.documentElement;
            const previousScroll = scrollArea.current?.scrollTop ?? 0;
            element.style.width = `${Math.min(Math.max(rect.width, overlayOwner ? 128 : 150), viewport.clientWidth - 16)}px`;
            const below = viewport.clientHeight - rect.bottom - 14;
            const above = rect.top - 14;
            const up = element.scrollHeight > below && above > below;
            element.dataset.placement = up ? "top" : "bottom";
            const maxHeight = Math.max(
                40,
                Math.min(options.length > 8 ? 220 : 260, up ? above : below),
            );
            element.style.setProperty("--dropdown-max-height", `${maxHeight}px`);
            element.style.maxHeight = `${maxHeight}px`;
            element.style.left = `${Math.max(8, Math.min(rect.left, viewport.clientWidth - element.offsetWidth - 8))}px`;
            element.style.top = `${Math.max(8, up ? rect.top - element.offsetHeight - 6 : rect.bottom + 6)}px`;
            element.style.visibility = "visible";
            if (scrollArea.current) scrollArea.current.scrollTop = previousScroll;
        };
        update();
        const selected = menu.current?.querySelector<HTMLButtonElement>(
            '[aria-selected="true"]:not(:disabled)',
        );
        (
            selected ?? menu.current?.querySelector<HTMLButtonElement>("button:not(:disabled)")
        )?.focus();
        window.addEventListener("resize", update);
        window.addEventListener("scroll", update, true);
        return () => {
            window.removeEventListener("resize", update);
            window.removeEventListener("scroll", update, true);
        };
    }, [open, closing, overlayOwner, options.length]);

    useEffect(() => {
        if (!closing) return;
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        const timer = window.setTimeout(
            () => {
                setOpen(false);
                setClosing(false);
            },
            reducedMotion ? 0 : 180,
        );
        return () => window.clearTimeout(timer);
    }, [closing]);

    useEffect(() => {
        if (!open) return;
        const outside = (event: PointerEvent) => {
            if (
                event.target instanceof Node &&
                !root.current?.contains(event.target) &&
                !menu.current?.contains(event.target)
            )
                setClosing(true);
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [open]);

    function showOverlay(): void {
        if (disabled) return;
        setPortal(root.current?.closest("dialog") ?? document.body);
        setClosing(false);
        setOpen(true);
    }
    return (
        <div
            ref={root}
            className="ui-dropdown"
            onKeyDown={(event) => {
                if (event.key === "Escape" && open) {
                    event.preventDefault();
                    event.stopPropagation();
                    setClosing(true);
                    trigger.current?.focus();
                }
                if (event.key === "Tab" && open && !closing) {
                    trigger.current?.focus();
                    setClosing(true);
                }
                if (disabled) return;
                if (!["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) return;
                event.preventDefault();
                if (!open || closing) {
                    showOverlay();
                    return;
                }
                const items = Array.from(
                    menu.current?.querySelectorAll<HTMLButtonElement>("button:not(:disabled)") ??
                        [],
                );
                const index = items.findIndex((item) => item === document.activeElement);
                const next =
                    event.key === "Home"
                        ? 0
                        : event.key === "End"
                          ? items.length - 1
                          : (index + (event.key === "ArrowDown" ? 1 : -1) + items.length) %
                            items.length;
                items[next]?.focus();
            }}
        >
            <Button
                variant="outline"
                className="ui-dropdown__trigger"
                ref={trigger}
                aria-label={label}
                aria-haspopup="listbox"
                aria-expanded={open && !closing}
                aria-controls={open ? id : undefined}
                disabled={disabled}
                onClick={() => (open && !closing ? setClosing(true) : showOverlay())}
            >
                {options.find((option) => option.value === value)?.label ?? label}
                <Icon name="chevron" variant="outline" className="ui-dropdown__chevron" />
            </Button>
            {open &&
                createPortal(
                    <div
                        ref={menu}
                        id={id}
                        role="listbox"
                        popover={overlayOwner ? "manual" : undefined}
                        data-overlay-owner={overlayOwner}
                        aria-label={label}
                        aria-hidden={closing || undefined}
                        inert={closing}
                        className={`ui-dropdown__menu ui-menu-surface${closing ? " ui-dropdown__menu--closing" : ""}${options.length > 6 ? " ui-dropdown__menu--scrollable" : ""}`}
                    >
                        <div ref={scrollArea} className="ui-dropdown__scroll" role="presentation">
                            {options.map((option) => (
                                <MenuItem
                                    key={option.value}
                                    type="button"
                                    role="option"
                                    aria-selected={option.value === value}
                                    tabIndex={-1}
                                    disabled={option.disabled}
                                    onClick={() => {
                                        if (option.disabled || closing) return;
                                        onChange(option.value);
                                        setClosing(true);
                                        trigger.current?.focus();
                                    }}
                                >
                                    <span>{option.label}</span>
                                    {value === option.value && (
                                        <Icon
                                            name="check"
                                            variant="outline"
                                            className="ui-dropdown__selected"
                                        />
                                    )}
                                </MenuItem>
                            ))}
                        </div>
                    </div>,
                    portal,
                )}
        </div>
    );
}
