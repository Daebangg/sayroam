import type { components } from "../api/generated/schema.ts";

export type Notification = components["schemas"]["NotificationResponse"];
export type NotificationSummary = components["schemas"]["NotificationSummaryResponse"];
export type NotificationState = Readonly<{
    notifications: readonly Notification[];
    summary: NotificationSummary | null;
    open: boolean;
    unreadOnly: boolean;
    listLoaded: boolean;
    loading: boolean;
    loadingMore: boolean;
    hasMore: boolean;
    changing: boolean;
    error: "load" | "change" | null;
    arrivalVersion: number;
}>;
