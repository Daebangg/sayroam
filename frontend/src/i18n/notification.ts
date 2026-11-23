import type { TFunction } from "i18next";
import type { components } from "@/api/generated/schema";

type NotificationMessage = Pick<components["schemas"]["NotificationResponse"], "type" | "content">;

export function localizeNotificationContent(
    notification: NotificationMessage,
    t: TFunction,
): string {
    if (notification.type === "WELCOME") return t("inbox.messages.welcome.content");
    return t("inbox.messages.match.content");
}
