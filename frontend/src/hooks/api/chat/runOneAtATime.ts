/**
 * 한 번에 하나만 돌린다. 도는 중에 다시 요청되면 끝난 뒤 한 번 더 돌린다.
 * 실행이 실패해도 그 사이 요청이 있었으면 다시 돌리고, 마지막 실행의 오류만 호출자에게 전달한다.
 */
export function runOneAtATime(task: () => Promise<void>, signal: AbortSignal): () => Promise<void> {
    let running: Promise<void> | null = null;
    let again = false;
    return () => {
        if (running) {
            again = true;
            return running;
        }
        running = (async () => {
            try {
                for (;;) {
                    again = false;
                    try {
                        await task();
                    } catch (error) {
                        if (!again || signal.aborted) throw error;
                        continue;
                    }
                    if (!again || signal.aborted) return;
                }
            } finally {
                running = null;
            }
        })();
        return running;
    };
}
