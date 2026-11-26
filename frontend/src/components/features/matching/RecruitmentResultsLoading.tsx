import { useTranslation } from "react-i18next";
import { Spinner } from "@/components/ui";

export function RecruitmentResultsLoading() {
    const { t } = useTranslation();
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className="roam-preview roam-preview--discovery roam-results-loading"
        >
            <Spinner label={t("matchingFlow.loadingResults")} />
        </main>
    );
}
