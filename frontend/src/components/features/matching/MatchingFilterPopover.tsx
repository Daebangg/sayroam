import { useEffect, useId, useRef, type ReactNode } from "react";
import { Button, Icon } from "@/components/ui";

type MatchingFilterPopoverProps = {
    label: string;
    fieldLabel?: string;
    /** 함수로 넘기면 선택을 마친 내용이 팝오버를 닫을 수 있다. 닫으면 포커스는 트리거로 돌아간다. */
    children: ReactNode | ((close: () => void) => ReactNode);
    className?: string;
    /** 패널 내부 구성의 스타일. 기본은 목록 필터용 밀도를 사용한다. */
    panelClassName?: string;
    icon?: "calendar" | "journey-age";
    /** 기본은 트리거 아래. top이어도 위 공간이 모자라고 아래가 더 넓으면 아래에 띄운다. */
    placement?: "top" | "bottom";
};

export function MatchingFilterPopover({
    label,
    fieldLabel,
    children,
    className = "",
    panelClassName = "matching-filter-popover ui-menu-surface",
    icon,
    placement = "bottom",
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
            if (fieldLabel) {
                const gap = 8;
                const below = window.innerHeight - rect.bottom - 2 * gap;
                const above = rect.top - 2 * gap;
                const height = element.offsetHeight;
                const openAbove = height > below && above > below;
                const left = rect.left + (rect.width - element.offsetWidth) / 2;
                element.style.left = `${Math.max(gap, Math.min(left, window.innerWidth - element.offsetWidth - gap))}px`;
                const top = openAbove ? rect.top - height - gap : rect.bottom + gap;
                element.style.top = `${Math.max(gap, Math.min(top, window.innerHeight - height - gap))}px`;
                return;
            }
            element.style.left = `${Math.max(8, Math.min(rect.left, window.innerWidth - element.offsetWidth - 8))}px`;
            const above = rect.top - 8 - element.offsetHeight;
            const up =
                placement === "top" && (above >= 8 || rect.top > window.innerHeight - rect.bottom);
            element.style.top = `${Math.max(8, Math.min(up ? above : rect.bottom + 8, window.innerHeight - element.offsetHeight - 8))}px`;
        };
        element.addEventListener("toggle", position);
        window.addEventListener("resize", position);
        window.addEventListener("scroll", position, true);
        return () => {
            element.removeEventListener("toggle", position);
            window.removeEventListener("resize", position);
            window.removeEventListener("scroll", position, true);
        };
    }, [placement, fieldLabel]);
    // 렌더 중 넘기는 함수라 ref 대신 id로 찾는다.
    function close(): void {
        const element = document.getElementById(id);
        if (element?.matches(":popover-open")) element.hidePopover();
        document.getElementById(`${id}-trigger`)?.focus({ preventScroll: true });
    }
    return (
        <>
            <Button
                ref={trigger}
                id={`${id}-trigger`}
                variant={fieldLabel ? "plain" : "outline"}
                className={`${fieldLabel ? "" : "ui-dropdown__trigger"} ${className}`}
                popoverTarget={id}
            >
                {icon && <Icon name={icon} className="matching-filter-icon" />}
                {fieldLabel ? (
                    <span className="matching-filter-field">
                        <small>{fieldLabel}</small>
                        {label}
                    </span>
                ) : (
                    label
                )}
                <Icon name="chevron" variant="outline" className="ui-dropdown__chevron" />
            </Button>
            <div
                ref={panel}
                id={id}
                popover="auto"
                className={`filter-popover ${panelClassName}`}
                role="dialog"
                aria-label={label}
            >
                {typeof children === "function" ? children(close) : children}
            </div>
        </>
    );
}
