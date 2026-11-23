import { useTranslation } from "react-i18next";
import { Badge } from "@/components/ui";
import type { MatchingRegistration } from "@/hooks/matching.types";

type RecruitmentStatus = MatchingRegistration["status"] | "MATCHED";

const badges = {
    ACTIVE: { label: "matchingFlow.active", tone: "brand" },
    MATCHED: { label: "roamMatePreview.matched", tone: "success" },
    CLOSED: { label: "matchingFlow.closed", tone: "danger" },
} as const;

export function RecruitmentStatusBadge({ status }: { status: RecruitmentStatus }) {
    const { t } = useTranslation();
    const { label, tone } = badges[status];
    return <Badge className="recruitment-status-badge" tone={tone} label={t(label)} />;
}
