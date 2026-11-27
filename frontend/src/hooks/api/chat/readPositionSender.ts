export type SenderTimers = {
    now: () => number;
    setTimeout: (run: () => void, ms: number) => number;
    clearTimeout: (id: number) => void;
};

export type ReadPositionSender = {
    mark: (messageId: number) => void;
    flush: () => void;
    dispose: () => void;
};

const browserTimers: SenderTimers = {
    now: () => Date.now(),
    setTimeout: (run, ms) => window.setTimeout(run, ms),
    clearTimeout: (id) => window.clearTimeout(id),
};

export function createReadPositionSender(
    send: (messageId: number) => Promise<void>,
    { idleMs, maxWaitMs }: { idleMs: number; maxWaitMs: number },
    timers: SenderTimers = browserTimers,
): ReadPositionSender {
    let pending: number | null = null;
    let sent = 0;
    let waitingSince: number | null = null;
    let timer: number | null = null;

    function stopTimer() {
        if (timer !== null) timers.clearTimeout(timer);
        timer = null;
    }

    function flush() {
        stopTimer();
        waitingSince = null;
        if (pending === null) return;
        const messageId = pending;
        const previous = sent;
        pending = null;
        sent = messageId;
        send(messageId).catch(() => {
            if (sent === messageId) sent = previous;
        });
    }

    return {
        mark(messageId) {
            if (messageId <= Math.max(sent, pending ?? 0)) return;
            pending = messageId;
            const now = timers.now();
            waitingSince ??= now;
            stopTimer();
            timer = timers.setTimeout(flush, Math.min(idleMs, maxWaitMs - (now - waitingSince)));
        },
        flush,
        dispose() {
            stopTimer();
            pending = null;
        },
    };
}
