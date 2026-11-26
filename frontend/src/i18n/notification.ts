import type { TFunction } from "i18next";
import type { components } from "@/api/generated/schema";

type NotificationMessage = Pick<components["schemas"]["NotificationResponse"], "type" | "content">;

export const notificationMessageKeys = {
    WELCOME: "welcome",
    COMPANION_MATCH_FOUND: "match",
    COMPANION_REQUEST: "request",
    COMPANION_ACCEPTED: "accepted",
    COMPANION_REJECTED: "rejected",
} as const satisfies Record<NotificationMessage["type"], string>;

export function localizeNotificationContent(
    notification: NotificationMessage,
    t: TFunction,
): string {
    return t(`inbox.messages.${notificationMessageKeys[notification.type]}.content`);
}
