import { useEffect, useEffectEvent, useRef, useState, useSyncExternalStore } from "react";
import { createPortal } from "react-dom";

export type ToastTone = "success" | "error" | "info";
type ToastProps = {
    message: string;
    onDismiss: () => void;
    tone?: ToastTone;
    durationMs?: number;
};

function getToastHost(): Element {
    return document.querySelector('dialog[data-modal-active="true"]') ?? document.body;
}

function subscribeToastHost(notify: () => void): () => void {
    const observer = new MutationObserver(notify);
    observer.observe(document.body, {
        childList: true,
        subtree: true,
        attributes: true,
        attributeFilter: ["data-modal-active"],
    });
    return () => observer.disconnect();
}

export function Toast({ message, onDismiss, tone = "success", durationMs = 3000 }: ToastProps) {
    const ref = useRef<HTMLDivElement>(null);
    const [entered, setEntered] = useState(false);
    const [closing, setClosing] = useState(false);
    // 활성 모달 밖의 DOM은 inert 상태이므로 모달의 열림·닫힘에 따라 포털 위치를 맞춘다.
    const host = useSyncExternalStore(subscribeToastHost, getToastHost);
    const dismiss = useEffectEvent(onDismiss);

    useEffect(() => {
        const element = ref.current;
        // 모달이 열려 있어도 같은 최상위 표시 계층에서 안내한다. 포커스는 옮기지 않는다.
        if (element?.isConnected) element.showPopover();
        return () => {
            if (element?.matches(":popover-open")) element.hidePopover();
        };
    }, [host]);
    useEffect(() => {
        const timer = window.setTimeout(() => setClosing(true), durationMs);
        return () => window.clearTimeout(timer);
    }, [durationMs]);
    useEffect(() => {
        if (!closing) return;
        const reducedMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
        const timer = window.setTimeout(() => dismiss(), reducedMotion ? 0 : 200);
        return () => window.clearTimeout(timer);
    }, [closing]);

    return createPortal(
        <div
            ref={ref}
            popover="manual"
            className={`ui-toast ui-toast--${tone}${closing ? " ui-toast--closing" : entered ? "" : " ui-toast--entering"}`}
            onAnimationEnd={(event) => {
                if (event.animationName === "toast-enter") setEntered(true);
            }}
            role="status"
            aria-live="polite"
            aria-atomic="true"
        >
            <p className="ui-toast__message">{message}</p>
        </div>,
        host,
    );
}
