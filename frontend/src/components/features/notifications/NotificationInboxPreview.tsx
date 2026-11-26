import { useLocation } from "react-router";
import { matchingNotificationState } from "@/routing/paths";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { mockupRequestUrl, mockupUrl } from "@/routing/mockups";
import { previewRequestIds } from "@/data/preview";
import { NotificationInbox, type InboxItem } from "./NotificationInbox";

type PreviewItem = {
    id: string;
    sample: "match" | "welcome" | "request" | "accepted";
    icon: InboxItem["icon"];
    section: InboxItem["section"];
    time: "justNow" | "fiveMinutes" | "halfHour" | "oneHour" | "yesterday";
};

const samples: readonly PreviewItem[] = [
    { id: "request", sample: "request", icon: "mail", section: "today", time: "justNow" },
    { id: "accepted", sample: "accepted", icon: "check", section: "today", time: "fiveMinutes" },
    { id: "match", sample: "match", icon: "matching", section: "today", time: "halfHour" },
    { id: "welcome", sample: "welcome", icon: "compass", section: "earlier", time: "yesterday" },
];

export function NotificationInboxPreview({
    simulateArrival = false,
    inline = false,
}: {
    simulateArrival?: boolean;
    inline?: boolean;
}) {
    const location = useLocation();
    const { t } = useTranslation();
    const [readIds, setReadIds] = useState(() => new Set<string>(["welcome"]));
    const [deletedIds, setDeletedIds] = useState(() => new Set<string>());
    const [arrivals, setArrivals] = useState<PreviewItem[]>([]);
    useEffect(() => {
        if (!simulateArrival) return;
        const timer = window.setTimeout(() => {
            setArrivals([
                {
                    id: "preview-arrival",
                    sample: "match",
                    icon: "matching",
                    section: "today",
                    time: "justNow",
                },
            ]);
        }, 4000);
        return () => window.clearTimeout(timer);
    }, [simulateArrival]);
    const records = [...arrivals, ...samples];
    const items: InboxItem[] = records
        .filter((item) => !deletedIds.has(item.id))
        .map((item) => ({
            id: item.id,
            icon: item.icon,
            section: item.section,
            title: t(`inbox.messages.${item.sample}.title`),
            content: t(`inbox.messages.${item.sample}.content`),
            ...(item.sample === "match" && { context: t("inbox.samples.match.context") }),
            time: t(`inbox.time.${item.time}`),
            isRead: readIds.has(item.id),
            ...(item.sample !== "welcome"
                ? {
                      action: {
                          state: matchingNotificationState(location),
                          to:
                              item.sample === "match"
                                  ? mockupUrl("matching-found")
                                  : mockupRequestUrl(
                                        item.sample === "request"
                                            ? previewRequestIds.received
                                            : previewRequestIds.accepted,
                                    ),
                      },
                  }
                : {}),
        }));

    return (
        <NotificationInbox
            items={items}
            inline={inline}
            onReadChange={(id, isRead) => {
                setReadIds((current) => {
                    const next = new Set(current);
                    if (isRead) next.add(id);
                    else next.delete(id);
                    return next;
                });
            }}
            onReadAll={() => setReadIds(new Set(records.map((item) => item.id)))}
            onDelete={(id) => setDeletedIds((current) => new Set([...current, id]))}
        />
    );
}
