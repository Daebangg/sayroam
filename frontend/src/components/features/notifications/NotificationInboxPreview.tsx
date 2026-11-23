import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { mockupUrl } from "@/routing/mockups";
import { localizeNotificationContent } from "@/i18n/notification";
import { NotificationInbox, type InboxItem } from "./NotificationInbox";

type PreviewItem = {
    id: string;
    sample: "match" | "welcome";
    icon: InboxItem["icon"];
    section: InboxItem["section"];
    time: "justNow" | "fiveMinutes" | "halfHour" | "oneHour" | "yesterday";
};

const samples: readonly PreviewItem[] = [
    { id: "match", sample: "match", icon: "matching", section: "today", time: "fiveMinutes" },
    { id: "welcome", sample: "welcome", icon: "compass", section: "earlier", time: "yesterday" },
];

export function NotificationInboxPreview({
    simulateArrival = false,
    inline = false,
}: {
    simulateArrival?: boolean;
    inline?: boolean;
}) {
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
            title:
                item.sample === "welcome"
                    ? t("inbox.messages.welcome.title")
                    : t("inbox.messages.match.title"),
            content:
                item.sample === "welcome"
                    ? localizeNotificationContent(
                          {
                              type: "WELCOME",
                              content: t("inbox.messages.welcome.content", { lng: "en" }),
                          },
                          t,
                      )
                    : t("inbox.messages.match.content"),
            ...(item.sample === "welcome"
                ? {}
                : { context: t(`inbox.samples.${item.sample}.context`) }),
            time: t(`inbox.time.${item.time}`),
            isRead: readIds.has(item.id),
            ...(item.sample === "match"
                ? {
                      action: {
                          to: mockupUrl("matching-found"),
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
