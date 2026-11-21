import { api } from "../../../api/api.ts";
import { getAuthRevision, subscribeAuthInvalidation } from "../../../api/auth/tokenStore.ts";
import type { Notification, NotificationState } from "../../notification.types.ts";

export const NOTIFICATION_POLLING = {
    closed: 60_000,
    open: 30_000,
    resumeStale: 15_000,
    maxRetry: 300_000,
    pageSize: 20,
} as const;

function initialState(): NotificationState {
    return {
        notifications: [],
        summary: null,
        open: false,
        unreadOnly: false,
        listLoaded: false,
        loading: false,
        loadingMore: false,
        hasMore: false,
        changing: false,
        error: null,
        arrivalVersion: 0,
    };
}

export function createNotificationController() {
    let state = initialState();
    let connected = false;
    let available = false;
    let revision = 0;
    let timer: ReturnType<typeof setTimeout> | undefined;
    let query: AbortController | null = null;
    let mutation: AbortController | null = null;
    let unsubscribe: (() => void) | undefined;
    let failures = 0;
    let nextAttempt = 0;
    let lastSuccess = 0;
    let highestId: number | null = null;
    let queued = false;
    let queuedList = false;
    const retainedReads = new Map<number, Notification>();
    const listeners = new Set<() => void>();

    function update(patch: Partial<NotificationState>): void {
        state = { ...state, ...patch };
        listeners.forEach((listener) => listener());
    }
    function live(): boolean {
        return connected && revision === getAuthRevision();
    }
    function valid(controller: AbortController): boolean {
        return live() && !controller.signal.aborted;
    }
    function clearTimer(): void {
        clearTimeout(timer);
        timer = undefined;
    }
    function cancelQuery(): void {
        query?.abort();
        query = null;
        queued = false;
        queuedList = false;
        update({ loading: false, loadingMore: false });
    }
    function schedule(delay?: number): void {
        clearTimer();
        if (!live() || !available || query || mutation) return;
        const interval = failures
            ? Math.min(NOTIFICATION_POLLING.closed * 2 ** failures, NOTIFICATION_POLLING.maxRetry)
            : state.open
              ? NOTIFICATION_POLLING.open
              : NOTIFICATION_POLLING.closed;
        const wait = delay ?? interval;
        nextAttempt = Date.now() + wait;
        timer = setTimeout(() => void refresh(), wait);
    }
    function observe(items: readonly Notification[]): void {
        const latest = items[0]?.id ?? 0;
        const previous = highestId;
        if (previous !== null && items.some((item) => item.id > previous && !item.isRead)) {
            update({ arrivalVersion: state.arrivalVersion + 1 });
        }
        highestId = Math.max(highestId ?? 0, latest);
    }
    function mergeHead(items: Notification[]): void {
        const oldest = items.at(-1)?.id ?? 0;
        const older =
            items.length === NOTIFICATION_POLLING.pageSize
                ? state.notifications.filter((item) => item.id < oldest)
                : [];
        const merged = new Map([...older, ...items].map((item) => [item.id, item]));
        if (state.unreadOnly) retainedReads.forEach((item, id) => merged.set(id, item));
        update({
            notifications: [...merged.values()].sort((a, b) => b.id - a.id),
            hasMore:
                items.length === NOTIFICATION_POLLING.pageSize &&
                (state.hasMore || state.notifications.length <= NOTIFICATION_POLLING.pageSize),
            listLoaded: true,
        });
    }
    async function list(signal: AbortSignal, unreadOnly: boolean, beforeId?: number) {
        return api.call("listMyNotifications", {
            params: {
                query: {
                    size: NOTIFICATION_POLLING.pageSize,
                    unreadOnly,
                    ...(beforeId === undefined ? {} : { beforeId }),
                },
            },
            signal,
        });
    }
    function finishQuery(controller: AbortController): void {
        if (query !== controller) return;
        query = null;
        update({ loading: false, loadingMore: false });
        if (queued) {
            const force = queuedList;
            queued = false;
            queuedList = false;
            void refresh(force);
        } else schedule();
    }
    async function refresh(forceList = false): Promise<void> {
        if (!live() || !available) return;
        if (query || mutation) {
            queued = true;
            queuedList ||= forceList;
            return;
        }
        clearTimer();
        const controller = new AbortController();
        query = controller;
        update({ loading: state.open && !state.listLoaded });
        try {
            // 열려 있을 때 목록과 개수는 독립적으로 병렬 조회한다.
            const [summary, visibleList] = await Promise.all([
                api.call("getMyNotificationSummary", { signal: controller.signal }),
                state.open ? list(controller.signal, state.unreadOnly) : Promise.resolve(null),
            ]);
            if (!valid(controller)) return;
            const changed =
                !state.summary ||
                summary.totalCount !== state.summary.totalCount ||
                summary.unreadCount !== state.summary.unreadCount;
            // summary에는 ID가 없으므로 최초 기준과 개수 변경 시 최신 ID를 별도로 확인한다.
            const head =
                visibleList ??
                (highestId === null || changed || forceList
                    ? await list(controller.signal, false)
                    : null);
            if (!valid(controller)) return;
            if (head) {
                observe(head.notifications);
                if (visibleList || !state.unreadOnly) mergeHead(head.notifications);
            }
            failures = 0;
            lastSuccess = Date.now();
            update({ summary, error: state.error === "load" ? null : state.error });
        } catch {
            if (valid(controller)) {
                failures++;
                update({ error: state.error === "change" ? "change" : "load" });
                controller.abort();
            }
        } finally {
            finishQuery(controller);
        }
    }
    function disconnect(): void {
        connected = false;
        clearTimer();
        unsubscribe?.();
        unsubscribe = undefined;
        cancelQuery();
        mutation?.abort();
        mutation = null;
        retainedReads.clear();
        highestId = null;
        failures = 0;
        nextAttempt = 0;
        lastSuccess = 0;
        update(initialState());
    }
    async function change(
        action: (signal: AbortSignal) => Promise<unknown>,
        apply: () => void,
    ): Promise<void> {
        if (!live() || mutation) return;
        clearTimer();
        cancelQuery();
        const controller = new AbortController();
        mutation = controller;
        update({ changing: true, error: null });
        try {
            await action(controller.signal);
            if (valid(controller)) apply();
        } catch {
            if (valid(controller)) update({ error: "change" });
        } finally {
            if (mutation === controller) {
                mutation = null;
                update({ changing: false });
                // 변경 요청은 반복하지 않고 조회로 실제 결과를 맞춘다.
                void refresh(true);
            }
        }
    }

    return {
        getSnapshot: (): NotificationState => state,
        subscribe(listener: () => void): () => void {
            listeners.add(listener);
            return () => {
                listeners.delete(listener);
            };
        },
        connect(isAvailable: boolean): () => void {
            connected = true;
            available = isAvailable;
            revision = getAuthRevision();
            unsubscribe = subscribeAuthInvalidation(disconnect);
            if (!available && !state.listLoaded) update({ error: "load" });
            void refresh();
            return disconnect;
        },
        disconnect,
        setAvailable(value: boolean): void {
            const resumed = !available && value;
            available = value;
            if (!live()) return;
            if (!value) {
                clearTimer();
                cancelQuery();
                if (!state.listLoaded) update({ error: "load" });
            } else if (resumed || Date.now() - lastSuccess >= NOTIFICATION_POLLING.resumeStale) {
                if (failures && nextAttempt > Date.now()) schedule(nextAttempt - Date.now());
                else void refresh();
            }
        },
        setOpen(value: boolean): void {
            if (state.open === value) return;
            update({ open: value });
            clearTimer();
            if (value) void refresh(true);
            else schedule();
        },
        setUnreadOnly(value: boolean): void {
            if (state.unreadOnly === value) return;
            cancelQuery();
            retainedReads.clear();
            update({ unreadOnly: value, notifications: [], listLoaded: false, hasMore: false });
            void refresh(true);
        },
        reload(): void {
            update({ error: null });
            void refresh(true);
        },
        async loadMore(): Promise<void> {
            if (!live() || !available || query || mutation || !state.hasMore) return;
            const beforeId = state.notifications.at(-1)?.id;
            if (beforeId === undefined) return;
            clearTimer();
            const controller = new AbortController();
            query = controller;
            update({ loadingMore: true });
            try {
                const result = await list(controller.signal, state.unreadOnly, beforeId);
                if (!valid(controller)) return;
                const merged = new Map(
                    [...state.notifications, ...result.notifications].map((item) => [
                        item.id,
                        item,
                    ]),
                );
                update({
                    notifications: [...merged.values()].sort((a, b) => b.id - a.id),
                    hasMore: result.notifications.length === NOTIFICATION_POLLING.pageSize,
                    error: state.error === "load" ? null : state.error,
                });
            } catch {
                if (valid(controller)) {
                    failures++;
                    update({ error: "load" });
                }
            } finally {
                finishQuery(controller);
            }
        },
        setRead(id: number, isRead: boolean): Promise<void> {
            const item = state.notifications.find((entry) => entry.id === id);
            if (!item || item.isRead === isRead) return Promise.resolve();
            return change(
                (signal) =>
                    api.call("updateNotificationReadStatus", {
                        params: { path: { notificationId: id } },
                        body: { isRead },
                        signal,
                    }),
                () => {
                    const updated = { ...item, isRead };
                    if (state.unreadOnly && isRead) retainedReads.set(id, updated);
                    else retainedReads.delete(id);
                    update({
                        notifications: state.notifications.map((entry) =>
                            entry.id === id ? updated : entry,
                        ),
                        summary: state.summary
                            ? {
                                  ...state.summary,
                                  unreadCount: Math.max(
                                      0,
                                      state.summary.unreadCount + (isRead ? -1 : 1),
                                  ),
                              }
                            : null,
                    });
                },
            );
        },
        readAll(): Promise<void> {
            return change(
                (signal) => api.call("readAllMyNotifications", { body: { isRead: true }, signal }),
                () => {
                    const items = state.notifications.map((item) => ({ ...item, isRead: true }));
                    if (state.unreadOnly) items.forEach((item) => retainedReads.set(item.id, item));
                    update({
                        notifications: items,
                        summary: state.summary ? { ...state.summary, unreadCount: 0 } : null,
                    });
                },
            );
        },
        delete(id: number): Promise<void> {
            return change(
                (signal) =>
                    api.call("deleteNotification", {
                        params: { path: { notificationId: id } },
                        signal,
                    }),
                () => {
                    const item = state.notifications.find((entry) => entry.id === id);
                    retainedReads.delete(id);
                    update({
                        notifications: state.notifications.filter((entry) => entry.id !== id),
                        summary: state.summary
                            ? {
                                  totalCount: Math.max(0, state.summary.totalCount - 1),
                                  unreadCount: Math.max(
                                      0,
                                      state.summary.unreadCount - (item && !item.isRead ? 1 : 0),
                                  ),
                              }
                            : null,
                    });
                },
            );
        },
    };
}
