import { useEffect, useId, useRef, type ReactNode } from "react";
import { Button, Icon } from "@/components/ui";

type MatchingFilterPopoverProps = {
    label: string;
    /** 함수로 넘기면 선택을 마친 내용이 팝오버를 닫을 수 있다. 닫으면 포커스는 트리거로 돌아간다. */
    children: ReactNode | ((close: () => void) => ReactNode);
    className?: string;
    /** 패널을 붙일 화면 영역의 추가 클래스 */
    panelClassName?: string;
    icon?: "calendar" | "journey-age";
    /** 기본은 트리거 아래. top이어도 위 공간이 모자라고 아래가 더 넓으면 아래에 띄운다. */
    placement?: "top" | "bottom";
};

export function MatchingFilterPopover({
    label,
    children,
    className = "",
    panelClassName = "",
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
    }, [placement]);
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
                className={`matching-filter-popover ui-menu-surface ${panelClassName}`}
                role="dialog"
                aria-label={label}
            >
                {typeof children === "function" ? children(close) : children}
            </div>
        </>
    );
}
