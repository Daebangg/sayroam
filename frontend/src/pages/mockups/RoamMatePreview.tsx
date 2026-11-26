import { useState } from "react";
import { useNavigate, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { NotificationInboxPreview } from "@/components/features/notifications/NotificationInboxPreview";
import { RecruitmentDiscovery } from "@/components/features/matching/RecruitmentDiscovery";
import type { RequestDecision } from "@/hooks/matching.types";
import { roamMateCandidates } from "./roamMateCandidates";

type PreviewScreen =
    | "notifications"
    | "matching-searching"
    | "matching-found"
    | "matching-empty"
    | "matching-delayed";

type PreviewProps = {
    screen: PreviewScreen;
    searchId?: number;
    requestId?: number;
    candidates?: typeof roamMateCandidates;
    onEdit?: () => void;
    onKeepRecruiting?: () => void;
    onCloseRecruitment?: () => void;
    recruitmentTitle?: string | null;
};

// 모집 목업과 같은 수명으로, 이 탭에서 보낸 예시 요청만 보관한다.
let previewDecisions = new Map<string, RequestDecision>();
let previewSentPairs = new Set<string>();

export function RoamMatePreview({
    screen,
    candidates = roamMateCandidates,
    searchId,
    requestId,
    onEdit,
    onKeepRecruiting,
    onCloseRecruitment,
    recruitmentTitle,
}: PreviewProps) {
    const { t } = useTranslation();
    const location = useLocation();
    const navigate = useNavigate();
    const [sentPairs, setSentPairs] = useState(previewSentPairs);
    const [decisions, setDecisions] = useState(previewDecisions);
    if (screen === "notifications") {
        return (
            <main id="main-content" tabIndex={-1} className="roam-preview">
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
            initialRequestId={requestId}
            delayed={screen === "matching-delayed"}
            phase={
                screen === "matching-delayed"
                    ? "searching"
                    : screen === "matching-empty" || candidates.length === 0
                      ? "empty"
                      : "found"
            }
            showFindingIntro={screen === "matching-searching"}
            candidates={candidates.map((candidate) => {
                const sent = sentPairs.has(`${searchId}:${candidate.searchId}`);
                return {
                    ...candidate,
                    canRequest: !sent && !candidate.requestStatus,
                    requestStatus: sent
                        ? "PENDING"
                        : (decisions.get(`${searchId}:${candidate.requestId}`) ??
                          candidate.requestStatus ??
                          null),
                    direction: sent ? "SENT" : (candidate.direction ?? null),
                };
            })}
            requestActions={
                searchId === undefined
                    ? undefined
                    : {
                          respond(id, status) {
                              previewDecisions = new Map(previewDecisions).set(
                                  `${searchId}:${id}`,
                                  status,
                              );
                              setDecisions(previewDecisions);
                              return true;
                          },
                          send(candidateId) {
                              previewSentPairs = new Set(previewSentPairs).add(
                                  `${searchId}:${candidateId}`,
                              );
                              setSentPairs(previewSentPairs);
                              return true;
                          },
                      }
            }
            destination={t("roamMatePreview.found.jeju")}
            onBack={() => {
                const steps = location.state?.resultsBackSteps;
                void navigate(Number.isSafeInteger(steps) && steps > 0 ? -steps : -1);
            }}
            onEdit={onEdit}
            onKeepRecruiting={onKeepRecruiting}
            onCloseRecruitment={onCloseRecruitment}
            recruitmentTitle={recruitmentTitle}
        />
    );
}
