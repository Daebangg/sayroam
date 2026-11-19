import { useEffect, useId, useRef, type ReactNode } from "react";
import { Button, Icon } from "@/components/ui";

type MatchingFilterPopoverProps = {
    label: string;
    children: ReactNode;
    className?: string;
    icon?: "calendar" | "journey-age";
};

export function MatchingFilterPopover({
    label,
    children,
    className = "",
    icon,
}: MatchingFilterPopoverProps) {
    const id = useId();
    const trigger = useRef<HTMLButtonElement>(null);
    const panel = useRef<HTMLDivElement>(null);
    useEffect(() => {
        const element = panel.current;
        if (!element) return;
        const position = () => {
            if (!element.matches(":popover-open")) return;
            const rect = trigger.current?.getBoundingClientRect();
            if (!rect) return;
            element.style.left = `${Math.max(8, Math.min(rect.left, window.innerWidth - element.offsetWidth - 8))}px`;
            element.style.top = `${Math.max(8, Math.min(rect.bottom + 8, window.innerHeight - element.offsetHeight - 8))}px`;
        };
        element.addEventListener("toggle", position);
        window.addEventListener("resize", position);
        window.addEventListener("scroll", position, true);
        return () => {
            element.removeEventListener("toggle", position);
            window.removeEventListener("resize", position);
            window.removeEventListener("scroll", position, true);
        };
    }, []);
    return (
        <>
            <Button
                ref={trigger}
                variant="outline"
                className={`ui-dropdown__trigger ${className}`}
                popoverTarget={id}
            >
                {icon && <Icon name={icon} className="matching-filter-icon" />}
                {label}
                <Icon name="chevron" variant="outline" className="ui-dropdown__chevron" />
            </Button>
            <div
                ref={panel}
                id={id}
                popover="auto"
                className="matching-filter-popover ui-menu-surface"
                role="dialog"
                aria-label={label}
            >
                {children}
            </div>
        </>
    );
}
