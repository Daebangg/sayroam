import { useTranslation } from "react-i18next";
import { Button, Icon, Tooltip } from "@/components/ui";
import { formatDayDate, formatMonthDayParts } from "@/i18n/locale";
import type { TripSection } from "./tripDays";

type TripDayDockProps = {
    sections: TripSection[];
    activeKey: string;
    onSelect: (key: string) => void;
    chatOpen: boolean;
    onToggleChat: () => void;
    inviteOpen: boolean;
    onToggleInvite: () => void;
};

export function TripDayDock({
    sections,
    activeKey,
    onSelect,
    chatOpen,
    onToggleChat,
    inviteOpen,
    onToggleInvite,
}: TripDayDockProps) {
    const { t, i18n } = useTranslation();
    const language = i18n.resolvedLanguage ?? "ko";
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
                    className="trip-day-dock__item"
                    aria-label={t("tripDetail.chat")}
                    aria-pressed={chatOpen}
                    onClick={onToggleChat}
                >
                    <Icon name="conversation" />
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
