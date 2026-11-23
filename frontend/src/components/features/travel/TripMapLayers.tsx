import { useId, useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Checkbox, Icon, Tooltip } from "@/components/ui";
import { formatDayDate } from "@/i18n/locale";
import type { TripSection } from "./tripDays";

type TripMapLayersProps = {
    sections: TripSection[];
    visible: ReadonlySet<string>;
    onChange: (visible: ReadonlySet<string>) => void;
};

export function TripMapLayers({ sections, visible, onChange }: TripMapLayersProps) {
    const { t, i18n } = useTranslation();
    const [open, setOpen] = useState(false);
    const panelId = useId();
    const language = i18n.resolvedLanguage ?? "ko";
    const days = sections.filter((section) => section.day !== null);
    const shownDays = days.filter((section) => visible.has(section.key)).length;
    const summary =
        shownDays === days.length
            ? t("tripDetail.layersAll")
            : shownDays === 0
              ? t("tripDetail.layersNone")
              : t("tripDetail.layersCount", { count: shownDays });

    function sectionLabel(section: TripSection): string {
        return section.date === null
            ? t("tripDetail.unscheduled")
            : `${t("tripDetail.dayTitle", { day: section.day })} · ${formatDayDate(language, section.date)}`;
    }

    function toggle(key: string) {
        const next = new Set(visible);
        if (next.has(key)) next.delete(key);
        else next.add(key);
        onChange(next);
    }

    return (
        <div className="trip-map-layers">
            <Tooltip content={summary}>
                <Button
                    variant="plain"
                    className="trip-map-layers__trigger"
                    aria-label={`${t("tripDetail.layers")} · ${summary}`}
                    aria-expanded={open}
                    aria-controls={panelId}
                    onClick={() => setOpen((value) => !value)}
                >
                    <Icon name="calendar" />
                </Button>
            </Tooltip>
            {open && (
                <section
                    id={panelId}
                    className="trip-map-layers__panel"
                    aria-label={t("tripDetail.layers")}
                >
                    <header>
                        <h2>{t("tripDetail.layers")}</h2>
                        <Button
                            variant="plain"
                            className="trip-map-layers__close"
                            aria-label={t("tripDetail.close")}
                            onClick={() => setOpen(false)}
                        >
                            <Icon name="close" variant="outline" />
                        </Button>
                    </header>
                    <div className="trip-map-layers__actions">
                        <Button
                            variant="outline"
                            onClick={() =>
                                onChange(new Set(sections.map((section) => section.key)))
                            }
                        >
                            {t("tripDetail.selectAll")}
                        </Button>
                        <Button variant="outline" onClick={() => onChange(new Set())}>
                            {t("tripDetail.clearAll")}
                        </Button>
                    </div>
                    <ul className="trip-map-layers__list">
                        {sections.map((section) => (
                            <li key={section.key}>
                                <span
                                    className={`trip-pin-dot ${section.tone === null ? "trip-pin--unscheduled" : `trip-pin--tone-${section.tone}`}`}
                                    aria-hidden="true"
                                />
                                <Checkbox
                                    label={sectionLabel(section)}
                                    checked={visible.has(section.key)}
                                    onChange={() => toggle(section.key)}
                                />
                            </li>
                        ))}
                    </ul>
                </section>
            )}
        </div>
    );
}
