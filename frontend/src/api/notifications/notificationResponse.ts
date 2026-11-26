import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

type Notification = components["schemas"]["NotificationResponse"];

function record(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function integer(value: unknown, minimum = 0): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= minimum;
}

function notification(value: unknown): value is Notification {
    return (
        record(value) &&
        integer(value.id, 1) &&
        typeof value.content === "string" &&
        typeof value.isRead === "boolean" &&
        typeof value.createdAt === "string" &&
        value.createdAt.endsWith("Z") &&
        Number.isFinite(Date.parse(value.createdAt)) &&
        (value.type === "WELCOME" ||
            value.type === "COMPANION_MATCH_FOUND" ||
            value.type === "COMPANION_REQUEST" ||
            value.type === "COMPANION_ACCEPTED" ||
            value.type === "COMPANION_REJECTED") &&
        (value.targetType === null ||
            value.targetType === "COMPANION_SEARCH" ||
            value.targetType === "COMPANION_REQUEST") &&
        (value.targetId === null || integer(value.targetId, 1))
    );
}

export function validateNotificationList(
    value: unknown,
    status: number,
): components["schemas"]["NotificationListResponse"] {
    if (
        !record(value) ||
        !Array.isArray(value.notifications) ||
        !value.notifications.every(notification) ||
        !value.notifications.every(
            (item, index, items) => index === 0 || (items[index - 1]?.id ?? 0) > item.id,
        )
    )
        throw new ApiError("invalid-response", { status });
    return { notifications: value.notifications };
}

export function validateNotificationSummary(
    value: unknown,
    status: number,
): components["schemas"]["NotificationSummaryResponse"] {
    if (
        !record(value) ||
        !integer(value.totalCount) ||
        !integer(value.unreadCount) ||
        value.unreadCount > value.totalCount
    )
        throw new ApiError("invalid-response", { status });
    return { totalCount: value.totalCount, unreadCount: value.unreadCount };
}
