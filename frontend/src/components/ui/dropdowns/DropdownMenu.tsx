import { useEffect, useId, useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";

type DropdownOption = { value: string; label: string; disabled?: boolean };
type DropdownMenuProps = {
    label: string;
    value: string;
    options: readonly DropdownOption[];
    onChange: (value: string) => void;
    disabled?: boolean;
};

export function DropdownMenu({ label, value, options, onChange, disabled }: DropdownMenuProps) {
    const [open, setOpen] = useState(false);
    const [portal, setPortal] = useState<HTMLElement>(() => document.body);
    const root = useRef<HTMLDivElement>(null);
    const trigger = useRef<HTMLButtonElement>(null);
    const menu = useRef<HTMLDivElement>(null);
    const id = useId();

    useLayoutEffect(() => {
        if (!open) return;
        const update = () => {
            const rect = trigger.current?.getBoundingClientRect();
            const element = menu.current;
            if (!rect || !element) return;
            const viewport = document.documentElement;
            const previousScroll = element.scrollTop;
            element.style.width = `${Math.min(Math.max(rect.width, 180), viewport.clientWidth - 16)}px`;
            const below = viewport.clientHeight - rect.bottom - 14;
            const above = rect.top - 14;
            const up = element.scrollHeight > below && above > below;
            element.style.maxHeight = `${Math.max(40, Math.min(260, up ? above : below))}px`;
            element.style.left = `${Math.max(8, Math.min(rect.left, viewport.clientWidth - element.offsetWidth - 8))}px`;
            element.style.top = `${Math.max(8, up ? rect.top - element.offsetHeight - 6 : rect.bottom + 6)}px`;
            element.style.visibility = "visible";
            element.scrollTop = previousScroll;
        };
        update();
        const selected = menu.current?.querySelector<HTMLButtonElement>('[aria-selected="true"]');
        (
            selected ?? menu.current?.querySelector<HTMLButtonElement>("button:not(:disabled)")
        )?.focus();
        window.addEventListener("resize", update);
        window.addEventListener("scroll", update, true);
        return () => {
            window.removeEventListener("resize", update);
            window.removeEventListener("scroll", update, true);
        };
    }, [open]);

    useEffect(() => {
        if (!open) return;
        const outside = (event: PointerEvent) => {
            if (
                event.target instanceof Node &&
                !root.current?.contains(event.target) &&
                !menu.current?.contains(event.target)
            )
                setOpen(false);
        };
        document.addEventListener("pointerdown", outside);
        return () => document.removeEventListener("pointerdown", outside);
    }, [open]);

    function showOverlay(): void {
        setPortal(root.current?.closest("dialog") ?? document.body);
        setOpen(true);
    }
    return (
        <div
            ref={root}
            className="ui-dropdown"
            onKeyDown={(event) => {
                if (event.key === "Escape") {
                    event.preventDefault();
                    event.stopPropagation();
                    setOpen(false);
                    trigger.current?.focus();
                }
                if (event.key === "Tab") setOpen(false);
                if (!["ArrowDown", "ArrowUp", "Home", "End"].includes(event.key)) return;
                event.preventDefault();
                if (!open) {
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
                aria-expanded={open}
                aria-controls={open ? id : undefined}
                disabled={disabled}
                onClick={() => (open ? setOpen(false) : showOverlay())}
            >
                {options.find((option) => option.value === value)?.label ?? label}
                <Icon name="chevron" className="ui-dropdown__chevron" />
            </Button>
            {open &&
                createPortal(
                    <div
                        ref={menu}
                        id={id}
                        role="listbox"
                        aria-label={label}
                        className="ui-dropdown__menu ui-menu-surface"
                    >
                        {options.map((option) => (
                            <button
                                key={option.value}
                                type="button"
                                role="option"
                                aria-selected={option.value === value}
                                tabIndex={-1}
                                disabled={option.disabled}
                                onClick={() => {
                                    onChange(option.value);
                                    setOpen(false);
                                    trigger.current?.focus();
                                }}
                            >
                                <span>{option.label}</span>
                                {value === option.value && (
                                    <span className="ui-dropdown__selected" aria-hidden="true" />
                                )}
                            </button>
                        ))}
                    </div>,
                    portal,
                )}
        </div>
    );
}
