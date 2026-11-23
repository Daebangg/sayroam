import { useEffect, useEffectEvent } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router";
import type { MatchingRegistration } from "@/hooks/matching.types";
import { useRecruitmentResults } from "@/hooks/api/matching/useRecruitmentResults";
import { RecruitmentDiscovery } from "@/components/features/matching/RecruitmentDiscovery";
import { Button, PageState } from "@/components/ui";
import { pagePaths } from "@/routing/paths";

type ResultsProps = {
    post: MatchingRegistration;
    destination: string;
    from?: "create" | "list" | "edit" | "resume";
    reloadPost: () => void;
    busy: boolean;
    failed: boolean;
    close: (id: number) => Promise<boolean>;
};

export function RecruitmentResultsPage({
    post,
    destination,
    from,
    reloadPost,
    busy,
    failed,
    close,
}: ResultsProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const initialSearch = from === "create" || from === "edit" || from === "resume";
    const { state, retry } = useRecruitmentResults(
        post.id,
        post.matchingRevision,
        initialSearch,
        reloadPost,
    );
    function list() {
        return navigate(pagePaths.matching, { replace: true, state: { matchingView: "mine" } });
    }
    function back() {
        if (from) void navigate(-1);
        else void list();
    }
    const leaveUnavailablePost = useEffectEvent(() => {
        void list();
        reloadPost();
    });
    useEffect(() => {
        if (state.phase === "unavailable") leaveUnavailablePost();
    }, [state.phase]);

    if (state.phase === "unavailable") return null;
    if (state.phase === "error" || state.phase === "offline") {
        return (
            <PageState
                kind="error"
                title={t("matchingFlow.resultsUnavailable")}
                description={t(
                    state.phase === "offline"
                        ? "matchingFlow.resultsOffline"
                        : "matchingFlow.resultsRetryHint",
                )}
                actions={
                    <>
                        {state.phase === "error" && (
                            <Button onClick={retry}>{t("inbox.retry")}</Button>
                        )}
                        <Button variant="soft" onClick={list}>
                            {t("matchingEntry.mineTitle")}
                        </Button>
                    </>
                }
            />
        );
    }
    const candidates = state.phase === "loaded" ? state.candidates : [];
    return (
        <RecruitmentDiscovery
            phase={
                state.phase === "searching" || state.phase === "delayed"
                    ? "searching"
                    : candidates.length
                      ? "found"
                      : "empty"
            }
            showFindingIntro={initialSearch}
            delayed={state.phase === "delayed"}
            candidates={candidates}
            destination={destination}
            recruitmentTitle={post.title}
            busy={busy}
            failed={failed}
            onBack={back}
            onKeepRecruiting={() => void list()}
            onEdit={async () => {
                await list();
                await navigate(pagePaths.matching, {
                    state: { matchingView: "find", editId: post.id },
                });
            }}
            onCloseRecruitment={async () => {
                if (await close(post.id)) await list();
            }}
        />
    );
}
