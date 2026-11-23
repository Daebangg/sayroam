import { useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { useTheme } from "@/hooks/useTheme";
import { NotificationInboxPreview } from "@/components/features/notifications/NotificationInboxPreview";
import { RoamDiscoveryLandscape } from "@/components/features/matching/RoamDiscoveryLandscape";
import { RecruitmentDiscovery } from "@/components/features/matching/RecruitmentDiscovery";
import { roamMateCandidates } from "./roamMateCandidates";

type PreviewScreen =
    | "notifications"
    | "matching-searching"
    | "matching-found"
    | "matching-empty"
    | "matching-delayed";

type PreviewProps = {
    screen: PreviewScreen;
    candidates?: typeof roamMateCandidates;
    onEdit?: () => void;
    onKeepRecruiting?: () => void;
    onCloseRecruitment?: () => void;
    recruitmentTitle?: string | null;
};

export function RoamMatePreview({
    screen,
    candidates = roamMateCandidates,
    onEdit,
    onKeepRecruiting,
    onCloseRecruitment,
    recruitmentTitle,
}: PreviewProps) {
    const { t } = useTranslation();
    const { resolved } = useTheme();
    const navigate = useNavigate();
    if (screen === "notifications") {
        return (
            <main id="main-content" tabIndex={-1} className="roam-preview">
                {resolved === "dark" && <RoamDiscoveryLandscape subtle />}
                <header className="roam-preview__heading">
                    <h1>{t("common.notifications")}</h1>
                </header>
                <section className="roam-preview__inbox" aria-label={t("common.notifications")}>
                    <NotificationInboxPreview inline />
                </section>
            </main>
        );
    }

    return (
        <RecruitmentDiscovery
            delayed={screen === "matching-delayed"}
            phase={
                screen === "matching-delayed"
                    ? "searching"
                    : screen === "matching-empty" || candidates.length === 0
                      ? "empty"
                      : "found"
            }
            showFindingIntro={screen === "matching-searching"}
            candidates={candidates}
            destination={t("roamMatePreview.found.jeju")}
            onBack={() => void navigate(-1)}
            onEdit={onEdit}
            onKeepRecruiting={onKeepRecruiting}
            onCloseRecruitment={onCloseRecruitment}
            recruitmentTitle={recruitmentTitle}
        />
    );
}
