import { useEffect, useId, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { PageLeaveDialog } from "@/components/layout/PageLeaveDialog";
import {
    Button,
    Icon,
    DiscoveryWordmark,
    DropdownMenu,
    Card,
    Avatar,
    type IconName,
    Modal,
    Toast,
} from "@/components/ui";
import { DetailsSidebar } from "@/components/layout/DetailsSidebar";
import { CompanionResponseDialog } from "./CompanionResponseDialog";
import { CandidateRequestAction } from "./CandidateRequestAction";
import { RecruitmentActionDialog } from "@/components/features/matching/RecruitmentActionDialog";
import { RecruitmentDetailsPanel } from "@/components/features/matching/RecruitmentDetailsPanel";
import { RoamMatchCard } from "@/components/features/matching/RoamMatchCard";
import type {
    Candidate,
    CandidateResult,
    CompanionRequest,
    RequestDecision,
} from "@/hooks/matching.types";

type DiscoveryProps = {
    phase: "searching" | "found" | "empty";
    candidates: CandidateResult["candidates"];
    archived?: CompanionRequest[];
    destination: string;
    requestActions?:
        | {
              send: (candidateId: number) => boolean | Promise<boolean>;
              respond: (requestId: number, status: RequestDecision) => boolean | Promise<boolean>;
              responseBusyId?: number | undefined;
              responseUncertain?: ReadonlySet<number>;
              busyId?: number | undefined;
              uncertain?: ReadonlySet<number>;
              recheck?: () => void;
          }
        | undefined;
    initialRequestId?: number | undefined;
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

const statusOrder = ["ACCEPTED", "RECEIVED", "SENT", "UNSENT", "REJECTED", "EXPIRED"] as const;
type StatusGroup = (typeof statusOrder)[number];

const groupIcons = {
    ACCEPTED: "check",
    RECEIVED: "mail",
    SENT: "arrow",
    UNSENT: "matching",
    REJECTED: "close",
    EXPIRED: "calendar",
} as const satisfies Record<StatusGroup, IconName>;

function statusGroup(
    status: Candidate["requestStatus"],
    direction: Candidate["direction"],
): StatusGroup {
    if (status === "PENDING") return direction === "RECEIVED" ? "RECEIVED" : "SENT";
    return status ?? "UNSENT";
}

const MIN_FINDING_DURATION_MS = 4000;

const filterOptions = [
    ["ALL", "matchingFlow.style"],
    ["SIMILAR", "roamMatePreview.found.filterSimilar"],
    ["NEEDS_DISCUSSION", "roamMatePreview.found.filterDiscussion"],
] as const;

export function RecruitmentDiscovery({
    phase: resultPhase,
    candidates,
    archived = [],
    destination,
    onBack,
    initialRequestId,
    requestActions,
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
    const initialCandidate = candidates.find(
        (candidate) => initialRequestId !== undefined && candidate.requestId === initialRequestId,
    );
    const initialArchived = archived.find(
        (request) => initialRequestId !== undefined && request.requestId === initialRequestId,
    );
    const [chosenStatus, setStatusFilter] = useState<StatusGroup | "ALL" | undefined>(() =>
        initialCandidate
            ? statusGroup(initialCandidate.requestStatus, initialCandidate.direction)
            : initialArchived
              ? statusGroup(initialArchived.status, initialArchived.direction)
              : undefined,
    );

    const statusFilter = chosenStatus ?? "ALL";
    const [chosenStyle, setStyleFilter] = useState<string>();
    const [filtersChanged, setFiltersChanged] = useState(false);
    const [resultsReady, setResultsReady] = useState(
        () => window.matchMedia("(prefers-reduced-motion: reduce)").matches,
    );
    const styleFilter = chosenStyle ?? "ALL";
    const entries = [
        ...candidates.map((candidate) => ({
            group: statusGroup(candidate.requestStatus, candidate.direction),
            profile: candidate,
            candidate,
            archivedRequest: null,
        })),
        ...archived.map((request) => ({
            group: statusGroup(request.status, request.direction),
            profile: request.candidate,
            candidate: null,
            archivedRequest: request,
        })),
    ];
    const styleMatches = entries.filter(
        ({ profile }) =>
            styleFilter === "ALL" || profile?.travelStyleMatch?.category === styleFilter,
    );
    const groups = statusOrder.map((value) => ({
        value,
        entries: styleMatches.filter((item) => item.group === value),
    }));
    const visible = groups
        .filter((group) => statusFilter === "ALL" || group.value === statusFilter)
        .flatMap((group) => group.entries);
    const statusButtons = [
        { value: "ALL" as const, count: styleMatches.length },
        ...groups
            .filter(
                (group) =>
                    group.value !== "EXPIRED" || group.entries.length || statusFilter === "EXPIRED",
            )
            .map((group) => ({ value: group.value, count: group.entries.length })),
    ];

    const dialogTitleId = useId();
    const [recipientId, setRecipientId] = useState<number>();
    const [sentNotice, setSentNotice] = useState<number>();
    const recipient = candidates.find(
        (candidate) =>
            candidate.searchId === recipientId && candidate.canRequest && !candidate.requestStatus,
    );
    async function confirmRequest() {
        if (!recipient || !requestActions) return;
        setRecipientId(undefined);
        if (await requestActions.send(recipient.searchId)) setSentNotice(recipient.searchId);
    }
    const [response, setResponse] = useState<{ requestId: number; status: RequestDecision }>();
    const [responseNotice, setResponseNotice] = useState<RequestDecision>();
    const missingTitleId = useId();
    function prepareResponse(requestId: number, status: RequestDecision) {
        const entry = entries.find(
            (item) => (item.candidate?.requestId ?? item.archivedRequest?.requestId) === requestId,
        );
        if (!entry) return;
        setResponse({ requestId, status });
    }
    async function confirmResponse() {
        if (!response || !requestActions || requestActions.responseBusyId !== undefined) return;
        const { requestId, status } = response;
        if (await requestActions.respond(requestId, status)) {
            setResponseNotice(status);
        }
        setResponse(undefined);
    }
    function requestAction(
        candidate: Pick<
            Candidate,
            "searchId" | "requestId" | "requestStatus" | "direction" | "canRequest"
        >,
        placement: "card" | "details",
    ) {
        return (
            requestActions && (
                <CandidateRequestAction
                    onRespond={(id) => prepareResponse(id, "ACCEPTED")}
                    onReject={(id) => prepareResponse(id, "REJECTED")}
                    placement={placement}
                    candidate={candidate}
                    busyId={requestActions.busyId ?? requestActions.responseBusyId}
                    onApply={setRecipientId}
                    recheck={
                        requestActions.uncertain?.has(candidate.searchId) ||
                        (candidate.requestId !== null &&
                            candidate.requestId !== undefined &&
                            requestActions.responseUncertain?.has(candidate.requestId))
                            ? requestActions.recheck
                            : undefined
                    }
                />
            )
        );
    }
    const [confirmingClose, setConfirmingClose] = useState(false);
    const list = useRef<HTMLDivElement>(null);
    const [selectedId, setSelectedId] = useState<number | undefined>(
        () => initialCandidate?.searchId ?? initialArchived?.candidateSearchId,
    );
    const selectedEntry = entries.find(
        (item) =>
            (item.candidate?.searchId ?? item.archivedRequest?.candidateSearchId) === selectedId,
    );
    const selectedArchived = selectedEntry?.archivedRequest;
    const selected =
        selectedEntry?.candidate ??
        (selectedArchived?.candidate
            ? { ...selectedArchived.candidate, searchId: selectedArchived.candidateSearchId }
            : undefined);
    const selectedAction =
        selectedEntry?.candidate ??
        (selectedArchived
            ? {
                  searchId: selectedArchived.candidateSearchId,
                  requestId: selectedArchived.requestId,
                  requestStatus: selectedArchived.status,
                  direction: selectedArchived.direction,
              }
            : undefined);
    const responseEntry = entries.find(
        (item) =>
            (item.candidate?.requestId ?? item.archivedRequest?.requestId) === response?.requestId,
    );
    function resetList() {
        setFiltersChanged(true);
        setResultsReady(true);
        setSelectedId(undefined);
        list.current?.scrollTo({ top: 0 });
    }
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className={`roam-preview roam-preview--discovery${phase === "found" ? " roam-preview--found" : ""}${phase === "empty" ? " roam-preview--empty" : ""}${selectedEntry && resultsReady ? " roam-preview--with-panel" : ""}`}
        >
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
                            <div
                                className="ui-filter-chips roam-status-filters"
                                role="group"
                                aria-label={t("companionRequests.statusLabel")}
                            >
                                {statusButtons.map(({ value, count }) => (
                                    <Button
                                        key={value}
                                        variant="plain"
                                        className="ui-filter-chip roam-status-chip"
                                        data-request-status={value === "SENT" ? "PENDING" : value}
                                        data-request-action={
                                            value === "RECEIVED" ? "respond" : undefined
                                        }
                                        aria-pressed={statusFilter === value}
                                        onClick={() => {
                                            if (value !== statusFilter) {
                                                setStatusFilter(value);
                                                resetList();
                                            }
                                        }}
                                    >
                                        {value !== "ALL" && <Icon name={groupIcons[value]} />}
                                        {t("roamMatePreview.found.filterCount", {
                                            label:
                                                value === "ALL"
                                                    ? t("mockup.filters.all")
                                                    : t(`companionRequests.resultFilters.${value}`),
                                            count,
                                        })}
                                    </Button>
                                ))}
                            </div>
                            <DropdownMenu
                                icon="filter"
                                label={t("roamMatePreview.found.filterLabel")}
                                value={styleFilter}
                                options={filterOptions.map(([value, label]) => ({
                                    value,
                                    label: t(label),
                                }))}
                                onChange={(value) => {
                                    if (value !== styleFilter) {
                                        setStyleFilter(value);
                                        resetList();
                                    }
                                }}
                            />
                        </div>
                    )}
                </div>
                {(phase === "empty" || phase === "delayed") && (
                    <div className="roam-discovery-actions">
                        {phase === "empty" && onEdit && (
                            <Button variant="edit" onClick={onEdit}>
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
                {phase === "found" && visible.length === 0 && (
                    <div className="matching-filter-empty roam-filter-empty" role="status">
                        <div className="matching-filter-empty__illustration" aria-hidden="true">
                            <Icon name="search" />
                        </div>
                        <h3>{t("companionRequests.noFilterResults")}</h3>
                        <p>{t("companionRequests.adjustFilters")}</p>
                    </div>
                )}
                {phase === "found" && visible.length > 0 && (
                    <div
                        className="roam-match-scroll"
                        ref={list}
                        data-filtered={filtersChanged || undefined}
                        onAnimationEnd={(event) => {
                            if (
                                event.target === event.currentTarget &&
                                event.animationName === "roam-scroll-ready"
                            )
                                setResultsReady(true);
                        }}
                    >
                        <ul
                            className="roam-match-list"
                            aria-label={t("roamMatePreview.found.title")}
                        >
                            {visible.map(({ candidate, archivedRequest: request }) =>
                                candidate ? (
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
                                            requestAction={requestAction(candidate, "card")}
                                        />
                                    </li>
                                ) : (
                                    <li key={`request-${request.requestId}`}>
                                        <Card className="roam-match-card">
                                            <header className="roam-match-card__person">
                                                <Avatar
                                                    src={request.candidate?.profileImageUrl ?? null}
                                                />
                                                <strong>
                                                    {request.candidate?.nickname ??
                                                        t("companionRequests.missingProfile")}
                                                </strong>
                                            </header>
                                            <h3>
                                                <Button
                                                    variant="plain"
                                                    className="matching-registration__open"
                                                    onClick={() =>
                                                        setSelectedId(request.candidateSearchId)
                                                    }
                                                >
                                                    {request.candidate?.title ||
                                                        t("matchingFlow.untitled")}
                                                </Button>
                                            </h3>
                                            {request.candidate?.introduction && (
                                                <p>{request.candidate.introduction}</p>
                                            )}
                                            <div className="roam-match-card__request">
                                                {requestAction(
                                                    {
                                                        searchId: request.candidateSearchId,
                                                        requestId: request.requestId,
                                                        requestStatus: request.status,
                                                        direction: request.direction,
                                                    },
                                                    "card",
                                                )}
                                            </div>
                                        </Card>
                                    </li>
                                ),
                            )}
                        </ul>
                    </div>
                )}
            </div>
            {recipient && (
                <Modal
                    open
                    labelledBy={dialogTitleId}
                    onClose={() => setRecipientId(undefined)}
                    className="matching-end-dialog"
                >
                    <h2 id={dialogTitleId}>
                        {t("companionRequests.confirmSend", { nickname: recipient.nickname })}
                    </h2>
                    <p className="matching-end-dialog__title">
                        {recipient.title || t("matchingFlow.untitled")}
                    </p>
                    <div className="matching-end-dialog__actions">
                        <Button variant="cancel" onClick={() => setRecipientId(undefined)}>
                            {t("common.cancel")}
                        </Button>
                        <Button variant="confirm" onClick={() => void confirmRequest()}>
                            {t("companionRequests.apply")}
                        </Button>
                    </div>
                </Modal>
            )}
            {sentNotice !== undefined && (
                <Toast
                    key={sentNotice}
                    message={t("companionRequests.sentNotice")}
                    onDismiss={() => setSentNotice(undefined)}
                />
            )}
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
            {response && (
                <CompanionResponseDialog
                    status={response.status}
                    candidate={responseEntry?.profile ?? null}
                    busy={requestActions?.responseBusyId !== undefined}
                    onCancel={() => setResponse(undefined)}
                    onConfirm={() => void confirmResponse()}
                />
            )}
            {responseNotice && (
                <Toast
                    message={t(
                        responseNotice === "ACCEPTED"
                            ? "companionRequests.acceptedNotice"
                            : "companionRequests.rejectedNotice",
                    )}
                    onDismiss={() => setResponseNotice(undefined)}
                />
            )}
            {resultsReady && selectedArchived && !selected && (
                <DetailsSidebar
                    id="recruitment-details-panel"
                    titleId={missingTitleId}
                    closing={false}
                    onClose={() => setSelectedId(undefined)}
                    onExited={() => {}}
                >
                    <div className="matching-details-panel__body">
                        <h2 id={missingTitleId}>{t("companionRequests.missingProfile")}</h2>
                    </div>
                    <footer className="matching-details-panel__footer">
                        {selectedAction && requestAction(selectedAction, "details")}
                        <Button variant="close" onClick={() => setSelectedId(undefined)}>
                            {t("common.close")}
                        </Button>
                    </footer>
                </DetailsSidebar>
            )}
            <RecruitmentDetailsPanel
                details={
                    selected && resultsReady
                        ? {
                              candidate: selected,
                              requestAction:
                                  selectedAction && requestAction(selectedAction, "details"),
                              ...(requestActions && {
                                  ownRecruitmentTitle:
                                      recruitmentTitle || t("matchingFlow.untitled"),
                              }),
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
