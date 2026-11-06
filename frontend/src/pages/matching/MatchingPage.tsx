import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Card, DropdownMenu, Icon, Tag, Tooltip } from "@/components/ui";
import { formatDateRange, formatNumber } from "@/i18n/locale";
import { matchingPreviews } from "@/data/preview";

export function MatchingPage() {
    const { t, i18n } = useTranslation();
    const [status, setStatus] = useState("looking");

    return (
        <main id="main-content" tabIndex={-1} className="workspace-page workspace-page--list">
            <div className="page-heading">
                <h1>{t("matching.heading")}</h1>
                <DropdownMenu
                    label={t("common.filter")}
                    value={status}
                    options={[
                        { value: "looking", label: t("brand.looking") },
                        { value: "ready", label: t("brand.ready") },
                    ]}
                    onChange={setStatus}
                />
            </div>
            <div className="matching-list">
                {matchingPreviews.map((item) => (
                    <Card className="matching-card" key={item.id}>
                        <img src={item.image} alt="" />
                        <div className="matching-card__content">
                            <h2>{t(item.title)}</h2>
                            <p>
                                {formatDateRange(
                                    i18n.resolvedLanguage ?? "ko",
                                    item.startDate,
                                    item.endDate,
                                )}
                            </p>
                            <div className="matching-card__tags">
                                <Tag label={t("brand.tagTogether")} tone="primary" />
                            </div>
                            <span className="matching-card__people">
                                <Icon name="matching" />
                                {t("matching.people", {
                                    current: formatNumber(
                                        i18n.resolvedLanguage ?? "ko",
                                        item.participants,
                                    ),
                                    total: formatNumber(
                                        i18n.resolvedLanguage ?? "ko",
                                        item.capacity,
                                    ),
                                })}
                            </span>
                        </div>
                        <Tooltip content={t("common.bookmark")} className="matching-card__bookmark">
                            <Button
                                variant="ghost"
                                className="icon-button"
                                aria-label={t("common.bookmark")}
                            >
                                <Icon name="bookmark" />
                            </Button>
                        </Tooltip>
                    </Card>
                ))}
            </div>
        </main>
    );
}
