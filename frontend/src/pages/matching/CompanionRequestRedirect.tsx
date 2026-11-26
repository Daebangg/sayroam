import { Navigate, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Button, ButtonLink, PageState } from "@/components/ui";
import { RecruitmentResultsLoading } from "@/components/features/matching/RecruitmentResultsLoading";
import { useCompanionRequest } from "@/hooks/api/matching/useCompanionRequests";
import { recruitmentResultsUrl, pagePaths } from "@/routing/paths";

export function CompanionRequestRedirect({ id }: { id: number }) {
    const { t } = useTranslation();
    const location = useLocation();
    const { request, error, recheck } = useCompanionRequest(id);
    if (error)
        return (
            <PageState
                kind="error"
                title={t(`companionRequests.${error}`)}
                description={t("matchingFlow.resultsRetryHint")}
                actions={
                    <>
                        <Button onClick={recheck}>{t("companionRequests.recheck")}</Button>
                        <ButtonLink to={pagePaths.matching} state={{ matchingView: "mine" }}>
                            {t("matchingEntry.mineTitle")}
                        </ButtonLink>
                    </>
                }
            />
        );
    if (!request) return <RecruitmentResultsLoading />;
    return (
        <Navigate
            to={`${recruitmentResultsUrl(request.searchId)}&selectedRequestId=${request.requestId}`}
            state={location.state}
            replace
        />
    );
}
