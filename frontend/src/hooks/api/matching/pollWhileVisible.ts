// 응답이 끝난 뒤에만 다음 조회를 예약한다. 화면을 벗어나면 읽기 요청도 취소한다.
export function pollWhileVisible(
    read: (signal: AbortSignal) => Promise<number | null>,
    onOffline?: () => void,
) {
    let controller: AbortController | undefined;
    let timer: number | undefined;
    let disposed = false;
    async function refresh() {
        if (disposed || controller || document.hidden || !navigator.onLine) return;
        window.clearTimeout(timer);
        const request = new AbortController();
        controller = request;
        try {
            const delay = await read(request.signal);
            if (delay !== null && !request.signal.aborted)
                timer = window.setTimeout(() => void refresh(), delay);
        } finally {
            request.abort();
            if (controller === request) controller = undefined;
        }
    }
    function pause() {
        window.clearTimeout(timer);
        controller?.abort();
        controller = undefined;
    }
    function update() {
        if (document.hidden || !navigator.onLine) {
            pause();
            if (!navigator.onLine) onOffline?.();
        } else void refresh();
    }
    document.addEventListener("visibilitychange", update);
    window.addEventListener("focus", update);
    window.addEventListener("online", update);
    window.addEventListener("offline", update);
    update();
    return () => {
        disposed = true;
        pause();
        document.removeEventListener("visibilitychange", update);
        window.removeEventListener("focus", update);
        window.removeEventListener("online", update);
        window.removeEventListener("offline", update);
    };
}
