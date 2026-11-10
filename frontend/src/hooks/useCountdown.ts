import { useCallback, useSyncExternalStore } from "react";

export function useCountdown(deadline: number | undefined, active = true): number {
    const subscribe = useCallback(
        (notify: () => void): (() => void) => {
            if (!active || deadline === undefined || deadline <= Date.now()) return () => {};
            const timer = window.setInterval(() => {
                notify();
                if (deadline <= Date.now()) window.clearInterval(timer);
            }, 1000);
            return () => window.clearInterval(timer);
        },
        [active, deadline],
    );
    const snapshot = useCallback(
        (): number =>
            active && deadline !== undefined
                ? Math.max(0, Math.ceil((deadline - Date.now()) / 1000))
                : 0,
        [active, deadline],
    );
    return useSyncExternalStore(subscribe, snapshot, () => 0);
}
