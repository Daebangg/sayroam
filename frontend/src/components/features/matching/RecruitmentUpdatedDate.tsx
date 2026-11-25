import { useTranslation } from "react-i18next";
import { Tooltip } from "@/components/ui";
import { formatDate } from "@/i18n/locale";

export function RecruitmentUpdatedDate({ updatedAt }: { updatedAt: string }) {
    const { t, i18n } = useTranslation();
    if (!updatedAt) return null;
    return (
        <Tooltip
            className="recruitment-updated-date"
            content={t("matchingFlow.lastUpdated")}
            touchHint
        >
            <time dateTime={updatedAt} tabIndex={0}>
                {formatDate(i18n.resolvedLanguage ?? "ko", updatedAt)}
            </time>
        </Tooltip>
    );
}
