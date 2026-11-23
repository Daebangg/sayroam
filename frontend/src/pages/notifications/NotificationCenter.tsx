import { useTranslation } from "react-i18next";
import {
    NotificationInbox,
    type InboxItem,
} from "@/components/features/notifications/NotificationInbox";
import { useNotifications } from "@/hooks/api/notifications/useNotifications";
import { localizeNotificationContent } from "@/i18n/notification";
import { formatDate } from "@/i18n/locale";
import { recruitmentResultsUrl } from "@/routing/paths";

export function NotificationCenter({ enabled }: { enabled: boolean }) {
    const inbox = useNotifications(enabled);
    const { t, i18n } = useTranslation();
    const now = new Date();
    const items: InboxItem[] = inbox.notifications.map((item) => {
        const date = new Date(item.createdAt);
        const match = item.type === "COMPANION_MATCH_FOUND";
        const context = match
            ? /^New travel companion candidates were found for "([\s\S]+)"\.$/.exec(
                  item.content,
              )?.[1]
            : undefined;
        return {
            id: String(item.id),
            title: t(match ? "inbox.messages.match.title" : "inbox.messages.welcome.title"),
            content: localizeNotificationContent(item, t),
            time: formatDate(i18n.resolvedLanguage ?? "ko", item.createdAt),
            section: date.toDateString() === now.toDateString() ? "today" : "earlier",
            icon: match ? "matching" : "compass",
            ...(match && item.targetType === "COMPANION_SEARCH" && item.targetId !== null
                ? { action: { to: recruitmentResultsUrl(item.targetId) } }
                : {}),
            ...(context && { context }),
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
