import { useTranslation } from "react-i18next";
import {
    NotificationInbox,
    type InboxItem,
} from "@/components/features/notifications/NotificationInbox";
import { useNotifications } from "@/hooks/api/notifications/useNotifications";
import { localizeNotificationContent } from "@/i18n/notification";
import { formatDate } from "@/i18n/locale";
import type { Notification } from "@/hooks/notification.types";

const icons = {
    WELCOME: "compass",
    COMPANION_REQUEST: "matching",
    COMPANION_ACCEPTED: "matching",
    TRIP_INVITE: "trips",
    EXPENSE_ADDED: "plan",
} as const satisfies Record<Notification["type"], InboxItem["icon"]>;

export function NotificationCenter({ enabled }: { enabled: boolean }) {
    const inbox = useNotifications(enabled);
    const { t, i18n } = useTranslation();
    const now = new Date();
    const items: InboxItem[] = inbox.notifications.map((item) => {
        const date = new Date(item.createdAt);
        return {
            id: String(item.id),
            title: t(`inbox.messages.${item.type === "WELCOME" ? "welcome" : item.type}.title`),
            content: localizeNotificationContent(item, t),
            time: formatDate(i18n.resolvedLanguage ?? "ko", item.createdAt),
            section: date.toDateString() === now.toDateString() ? "today" : "earlier",
            icon: icons[item.type],
            isRead: item.isRead,
        };
    });
    return (
        <NotificationInbox
            items={items}
            summary={inbox.summary}
            arrivalVersion={inbox.arrivalVersion}
            loading={inbox.loading}
            loaded={inbox.listLoaded}
            busy={inbox.changing}
            error={inbox.error}
            hasMore={inbox.hasMore}
            loadingMore={inbox.loadingMore}
            onOpenChange={inbox.setOpen}
            onFilterChange={inbox.setUnreadOnly}
            onRetry={inbox.reload}
            onLoadMore={() => void inbox.loadMore()}
            onReadChange={(id, isRead) => void inbox.setRead(Number(id), isRead)}
            onReadAll={() => void inbox.readAll()}
            onDelete={(id) => void inbox.delete(Number(id))}
        />
    );
}
