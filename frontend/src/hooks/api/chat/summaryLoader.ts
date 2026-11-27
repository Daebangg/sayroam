export type ChatSummary = { unreadCount: number; lastMessageId: number };

export type SummaryLoader = {
    /** 아직 받지 못했으면 조회한다. 조회 중이거나 이미 받았으면 아무것도 하지 않는다. */
    request: () => void;
    dispose: () => void;
};

/** 실패해도 스스로 다시 조회하지 않는다. 다음 request 때 다시 조회한다. */
export function createSummaryLoader(
    load: (signal: AbortSignal) => Promise<ChatSummary>,
    onLoaded: (summary: ChatSummary) => void,
): SummaryLoader {
    const controller = new AbortController();
    let state: "idle" | "loading" | "loaded" | "failed" = "idle";

    return {
        request() {
            if (controller.signal.aborted || state === "loading" || state === "loaded") return;
            state = "loading";
            load(controller.signal).then(
                (summary) => {
                    if (controller.signal.aborted) return;
                    state = "loaded";
                    onLoaded(summary);
                },
                () => {
                    if (!controller.signal.aborted) state = "failed";
                },
            );
        },
        dispose() {
            controller.abort();
        },
    };
}
