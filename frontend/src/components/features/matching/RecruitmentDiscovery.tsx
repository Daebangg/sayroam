import { useEffect, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { PageLeaveDialog } from "@/components/layout/PageLeaveDialog";
import { Button, Icon, DiscoveryWordmark, FilterChips } from "@/components/ui";
import { RoamDiscoveryLandscape } from "@/components/features/matching/RoamDiscoveryLandscape";
import { RecruitmentActionDialog } from "@/components/features/matching/RecruitmentActionDialog";
import { RecruitmentDetailsPanel } from "@/components/features/matching/RecruitmentDetailsPanel";
import { RoamMatchCard } from "@/components/features/matching/RoamMatchCard";
import type { CandidateResult } from "@/hooks/matching.types";

type DiscoveryProps = {
    phase: "searching" | "found" | "empty";
    candidates: CandidateResult["candidates"];
    destination: string;
    onBack: () => void;
    onEdit?: (() => void) | undefined;
    onKeepRecruiting?: (() => void) | undefined;
    onCloseRecruitment?: (() => void) | undefined;
    recruitmentTitle?: string | null | undefined;
    showFindingIntro?: boolean;
    delayed?: boolean;
    busy?: boolean;
    failed?: boolean;
};

const MIN_FINDING_DURATION_MS = 4000;

const filterOptions = [
    ["ALL", "mockup.filters.all"],
    ["SIMILAR", "roamMatePreview.found.filterSimilar"],
    ["NEEDS_DISCUSSION", "roamMatePreview.found.filterDiscussion"],
] as const;

export function RecruitmentDiscovery({
    phase: resultPhase,
    candidates,
    destination,
    onBack,
    onEdit,
    onKeepRecruiting,
    onCloseRecruitment,
    recruitmentTitle,
    showFindingIntro = false,
    delayed = false,
    busy = false,
    failed = false,
}: DiscoveryProps) {
    const { t } = useTranslation();
    const [introElapsed, setIntroElapsed] = useState(false);
    useEffect(() => {
        if (!showFindingIntro) return;
        const timer = window.setTimeout(() => setIntroElapsed(true), MIN_FINDING_DURATION_MS);
        return () => window.clearTimeout(timer);
    }, [showFindingIntro]);
    const phase =
        showFindingIntro && !introElapsed
            ? "searching"
            : delayed && resultPhase === "searching"
              ? "delayed"
              : resultPhase;
    useEffect(() => {
        document.title = `${t(`roamMatePreview.${phase}.title`)} · ${t("app.name")}`;
    }, [phase, t]);
    const candidateFilters = filterOptions.map(([value, label]) => ({
        value,
        label,
        candidates:
            value === "ALL"
                ? candidates
                : candidates.filter((candidate) => candidate.travelStyleMatch?.category === value),
    }));

    const [confirmingClose, setConfirmingClose] = useState(false);
    const [styleFilter, setStyleFilter] = useState<string>();
    const list = useRef<HTMLDivElement>(null);
    const activeFilter =
        candidateFilters.find(
            ({ value, candidates }) => value === styleFilter && candidates.length > 0,
        ) ?? candidateFilters[0]!;
    const visibleCandidates = activeFilter.candidates;
    const [selectedId, setSelectedId] = useState<number>();
    const selected = candidates.find((candidate) => candidate.searchId === selectedId);
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className={`roam-preview roam-preview--discovery${phase === "found" ? " roam-preview--found" : ""}${phase === "empty" ? " roam-preview--empty" : ""}${selected ? " roam-preview--with-panel" : ""}`}
        >
            <RoamDiscoveryLandscape />
            <div key={phase} className="roam-discovery-stage">
                {(phase === "found" || phase === "empty") && (
                    <Button
                        variant="plain"
                        className="settings-back roam-discovery-back"
                        onClick={onBack}
                    >
                        <Icon name="arrow" variant="outline" />
                        {t("matchingEntry.back")}
                    </Button>
                )}
                <div className="roam-discovery-header">
                    <div
                        className="roam-discovery-message"
                        role="status"
                        aria-live="polite"
                        aria-atomic="true"
                    >
                        <h1 className="roam-discovery-heading">
                            <span className="visually-hidden">
                                {t(`roamMatePreview.${phase}.title`)}
                            </span>
                            <DiscoveryWordmark state={phase} />
                        </h1>
                        <p className="roam-discovery-description">
                            {t(`roamMatePreview.${phase}.description`)}
                        </p>
                    </div>
                    {phase === "found" && (
                        <div className="roam-match-filters">
                            <FilterChips
                                label={t("roamMatePreview.found.filterLabel")}
                                value={activeFilter.value}
                                options={candidateFilters.map(({ value, label, candidates }) => ({
                                    value,
                                    label: t("roamMatePreview.found.filterCount", {
                                        label: t(label),
                                        count: candidates.length,
                                    }),
                                    disabled: candidates.length === 0,
                                }))}
                                onChange={(value) => {
                                    if (value === activeFilter.value) return;
                                    setStyleFilter(value);
                                    setSelectedId(undefined);
                                    list.current?.scrollTo({ top: 0 });
                                }}
                            />
                        </div>
                    )}
                </div>
                {(phase === "empty" || phase === "delayed") && (
                    <div className="roam-discovery-actions">
                        {phase === "empty" && onEdit && (
                            <Button variant="soft" onClick={onEdit}>
                                {t("roamMatePreview.empty.edit")}
                            </Button>
                        )}
                        <Button variant="highlight" onClick={onKeepRecruiting}>
                            {t("roamMatePreview.empty.keep")}
                        </Button>
                        {onCloseRecruitment && (
                            <Button variant="danger-soft" onClick={() => setConfirmingClose(true)}>
                                {t("roamMatePreview.empty.close")}
                            </Button>
                        )}
                    </div>
                )}
                {phase === "found" && (
                    <div ref={list} className="roam-match-scroll">
                        <ul
                            key={styleFilter ?? "initial"}
                            className="roam-match-list"
                            data-filtered={styleFilter !== undefined || undefined}
                            aria-label={t("roamMatePreview.found.title")}
                        >
                            {visibleCandidates.map((candidate) => (
                                <li key={candidate.searchId}>
                                    <RoamMatchCard
                                        candidate={candidate}
                                        destination={destination}
                                        selected={selectedId === candidate.searchId}
                                        onSelect={() =>
                                            setSelectedId(
                                                selectedId === candidate.searchId
                                                    ? undefined
                                                    : candidate.searchId,
                                            )
                                        }
                                    />
                                </li>
                            ))}
                        </ul>
                    </div>
                )}
            </div>
            {confirmingClose && onCloseRecruitment && (
                <RecruitmentActionDialog
                    action="close"
                    title={recruitmentTitle || t("matchingFlow.untitled")}
                    hasMatches={phase === "found"}
                    busy={busy}
                    failed={failed}
                    onCancel={() => setConfirmingClose(false)}
                    onConfirm={onCloseRecruitment}
                />
            )}
            {phase === "searching" && (
                <PageLeaveDialog
                    shouldBlock={({ currentLocation, nextLocation }) =>
                        currentLocation.pathname !== nextLocation.pathname ||
                        currentLocation.search !== nextLocation.search
                    }
                    title={t("roamMatePreview.searching.leaveTitle")}
                    description={t("roamMatePreview.searching.leaveHint")}
                    stayLabel={t("roamMatePreview.searching.stay")}
                    leaveLabel={t("profileSetup.leave")}
                />
            )}
            <RecruitmentDetailsPanel
                details={
                    selected
                        ? {
                              candidate: selected,
                              destination: destination,
                              onClose: () => setSelectedId(undefined),
                              busy: false,
                              failed: false,
                          }
                        : undefined
                }
            />
        </main>
    );
}
