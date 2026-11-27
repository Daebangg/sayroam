import { useTranslation } from "react-i18next";
import { Button, Icon, NotificationBadge, Tooltip } from "@/components/ui";
import { formatDayDate, formatMonthDayParts } from "@/i18n/locale";
import type { TripSection } from "./tripDays";

type TripDayDockProps = {
    sections: TripSection[];
    activeKey: string;
    onSelect: (key: string) => void;
    chatOpen: boolean;
    /** 패널을 닫아 둔 동안 다른 회원이 보낸 안 읽은 메시지 개수 */
    chatUnreadCount: number;
    onToggleChat: () => void;
    inviteOpen: boolean;
    onToggleInvite: () => void;
};

export function TripDayDock({
    sections,
    activeKey,
    onSelect,
    chatOpen,
    chatUnreadCount,
    onToggleChat,
    inviteOpen,
    onToggleInvite,
}: TripDayDockProps) {
    const { t, i18n } = useTranslation();
    const language = i18n.resolvedLanguage ?? "ko";
    const chatLabel =
        chatUnreadCount > 0
            ? t("tripDetail.chatUnread", { count: chatUnreadCount })
            : t("tripDetail.chat");
    return (
        <nav className="trip-day-dock" aria-label={t("tripDetail.dockLabel")}>
            {sections.map((section) => {
                const parts =
                    section.date === null
                        ? {
                              month: t("tripDetail.dockUnscheduledTop"),
                              day: t("tripDetail.dockUnscheduledBottom"),
                          }
                        : formatMonthDayParts(language, section.date);
                const label =
                    section.date === null
                        ? t("tripDetail.unscheduled")
                        : `${t("tripDetail.dayTitle", { day: section.day })} · ${formatDayDate(language, section.date)}`;
                return (
                    <Button
                        key={section.key}
                        variant="plain"
                        className="trip-day-dock__item trip-day-dock__day"
                        aria-label={label}
                        aria-current={section.key === activeKey ? "location" : undefined}
                        onClick={() => onSelect(section.key)}
                    >
                        <span aria-hidden="true">{parts.month}</span>
                        <span aria-hidden="true">{parts.day}</span>
                    </Button>
                );
            })}
            <span className="trip-day-dock__divider" aria-hidden="true" />
            <Tooltip content={t("tripDetail.chat")}>
                <Button
                    variant="plain"
                    className="trip-day-dock__item trip-day-dock__chat"
                    aria-label={chatLabel}
                    aria-pressed={chatOpen}
                    onClick={onToggleChat}
                >
                    <Icon name="conversation" />
                    {chatUnreadCount > 0 && (
                        <span className="trip-day-dock__badge" aria-hidden="true">
                            <NotificationBadge
                                count={chatUnreadCount}
                                maxCount={99}
                                label={chatLabel}
                            />
                        </span>
                    )}
                </Button>
            </Tooltip>
            <Tooltip content={t("tripDetail.invite")}>
                <Button
                    variant="plain"
                    className="trip-day-dock__item trip-day-dock__invite"
                    aria-label={t("tripDetail.invite")}
                    aria-pressed={inviteOpen}
                    onClick={onToggleInvite}
                >
                    <Icon name="matching" />
                    <Icon name="plus" variant="outline" className="trip-day-dock__plus" />
                </Button>
            </Tooltip>
        </nav>
    );
}
