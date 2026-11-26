import { RecruitmentStyleDetails } from "./RecruitmentStyleDetails";
import { RecruitmentUpdatedDate } from "./RecruitmentUpdatedDate";
import { RecruitmentTextAction } from "./RecruitmentTextAction";
import { type ReactNode, useEffect, useId, useRef, useState } from "react";
import { DetailsSidebar } from "@/components/layout/DetailsSidebar";
import { useTranslation } from "react-i18next";
import { Avatar, Button, Icon, Tag, Tooltip } from "@/components/ui";
import { RecruitmentStatusBadge } from "./RecruitmentStatusBadge";
import type { Candidate, CompanionRequest, MatchingRegistration } from "@/hooks/matching.types";

type PanelCandidate = NonNullable<CompanionRequest["candidate"]> &
    Pick<Candidate, "searchId"> &
    Partial<Pick<Candidate, "updatedAt" | "overlapStartDate" | "overlapEndDate" | "overlapDays">>;

type RecruitmentDetailsPanelProps = {
    destination: string;
    onClose: () => void;
    ownRecruitmentTitle?: string;
    requestAction?: ReactNode;
    hasCandidateMatches?: boolean;
    onViewResults?: () => void;
    onEdit?: () => void;
    onFinish?: () => void;
    onResume?: () => void;
    resumeDisabled?: boolean;
    resumeHint?: string;
    errorMessage?: string;
    busy: boolean;
    failed: boolean;
} & (
    | { registration: MatchingRegistration; candidate?: never }
    | { candidate: PanelCandidate; registration?: never }
);

export function RecruitmentDetailsPanel({
    details,
}: {
    details: RecruitmentDetailsPanelProps | undefined;
}) {
    // 선택이 해제돼도 닫힘 모션이 끝날 때까지 마지막 모집을 표시한다.
    const [lastDetails, setLastDetails] = useState(details);
    if (details && details !== lastDetails) setLastDetails(details);
    const visible = details ?? lastDetails;
    if (!visible) return null;
    return (
        <PanelContent {...visible} closing={!details} onExited={() => setLastDetails(undefined)} />
    );
}

function PanelContent({
    closing,
    onExited,
    registration,
    candidate,
    destination,
    onClose,
    onEdit,
    onViewResults,
    hasCandidateMatches = false,
    requestAction,
    ownRecruitmentTitle,
    onFinish,
    onResume,
    resumeDisabled = false,
    resumeHint,
    errorMessage,
    busy,
    failed,
}: RecruitmentDetailsPanelProps & { closing: boolean; onExited: () => void }) {
    const { t, i18n } = useTranslation();
    const post = registration ?? candidate;
    const postId = registration?.id ?? candidate?.searchId;
    const status = registration?.status;
    const titleId = useId();
    const content = useRef<HTMLDivElement>(null);
    useEffect(() => {
        content.current?.scrollTo({ top: 0 });
    }, [postId]);
    const date = (value: string) =>
        new Intl.DateTimeFormat(i18n.resolvedLanguage === "en" ? "en-US" : "ko-KR", {
            year: "numeric",
            month: "short",
            day: "numeric",
            timeZone: "UTC",
        }).format(new Date(`${value}T00:00:00Z`));
    return (
        <DetailsSidebar
            id="recruitment-details-panel"
            titleId={titleId}
            closing={closing}
            onClose={onClose}
            onExited={onExited}
        >
            <div ref={content} className="matching-details-panel__body">
                <header className="matching-details-panel__header">
                    {ownRecruitmentTitle && (
                        <p>{t("companionRequests.ownPost", { title: ownRecruitmentTitle })}</p>
                    )}
                    <div className="recruitment-heading-row">
                        {registration && (
                            <div className="matching-details-panel__status">
                                <RecruitmentStatusBadge
                                    status={hasCandidateMatches ? "MATCHED" : registration.status}
                                />
                                {status === "ACTIVE" && onFinish && (
                                    <RecruitmentTextAction
                                        action="close"
                                        disabled={busy}
                                        onClick={onFinish}
                                    />
                                )}
                                {status === "CLOSED" && onResume && (
                                    <Tooltip content={resumeHint || t("matchingFlow.resume")}>
                                        <RecruitmentTextAction
                                            action="resume"
                                            disabled={busy || resumeDisabled}
                                            onClick={onResume}
                                        />
                                    </Tooltip>
                                )}
                            </div>
                        )}
                        {candidate && (
                            <div className="matching-details-author">
                                <Avatar src={candidate.profileImageUrl} />
                                <strong>{candidate.nickname}</strong>
                            </div>
                        )}
                        <RecruitmentUpdatedDate updatedAt={post.updatedAt ?? ""} />
                    </div>
                    <h2 id={titleId} aria-live="polite">
                        {post.title || t("matchingFlow.untitled")}
                    </h2>
                    {post.introduction && (
                        <div className="matching-details-introduction">
                            <p>{post.introduction}</p>
                        </div>
                    )}
                </header>
                <dl className="matching-details-panel__meta">
                    <div>
                        <dt>
                            <Icon name="pin" />
                            {t("matchingFlow.destination")}
                        </dt>
                        <dd>{destination}</dd>
                    </div>
                    <div>
                        <dt>
                            <Icon name="calendar" />
                            {t("matchingFlow.period")}
                        </dt>
                        <dd className="matching-details-panel__dates">
                            <span>
                                {t("matchingFlow.dateRangeStart", {
                                    start: date(post.startDate),
                                })}
                            </span>
                            <span>{date(post.endDate)}</span>
                        </dd>
                    </div>
                </dl>
                <dl className="matching-details-panel__privacy">
                    <div>
                        <dt>
                            <Icon name="gender-unset" />
                            {t(
                                candidate
                                    ? "settings.gender"
                                    : "matchingFlow.genderVisibilityLabel",
                            )}
                        </dt>
                        <dd>
                            <Tag
                                label={
                                    candidate
                                        ? candidate.gender
                                            ? t(`settings.genderValues.${candidate.gender}`)
                                            : t("matchingFlow.private")
                                        : t(
                                              registration.showGender
                                                  ? "matchingFlow.public"
                                                  : "matchingFlow.private",
                                          )
                                }
                            />
                        </dd>
                    </div>
                    <div>
                        <dt>
                            <Icon name="journey-age" />
                            {t(
                                candidate
                                    ? "matchingFlow.myAge"
                                    : "matchingFlow.ageVisibilityLabel",
                            )}
                        </dt>
                        <dd>
                            <Tag
                                label={
                                    candidate
                                        ? candidate.age !== null
                                            ? t("matchingFlow.ageValue", { count: candidate.age })
                                            : t("matchingFlow.private")
                                        : t(
                                              registration.showAge
                                                  ? "matchingFlow.public"
                                                  : "matchingFlow.private",
                                          )
                                }
                            />
                        </dd>
                    </div>
                </dl>
                <dl
                    className={
                        candidate
                            ? "matching-conditions-summary matching-details-overlap"
                            : "matching-conditions-summary"
                    }
                >
                    {candidate ? (
                        candidate.overlapStartDate &&
                        candidate.overlapEndDate &&
                        candidate.overlapDays !== undefined && (
                            <div>
                                <dt>
                                    <Icon name="calendar" />
                                    {t("matchingFlow.overlap", { count: candidate.overlapDays })}
                                </dt>
                                <dd>
                                    {t("matchingFlow.dateRange", {
                                        start: date(candidate.overlapStartDate),
                                        end: date(candidate.overlapEndDate),
                                    })}
                                </dd>
                            </div>
                        )
                    ) : (
                        <>
                            <div>
                                <dt>
                                    <Icon
                                        name={
                                            registration.preferredGender === "MALE"
                                                ? "gender-male"
                                                : registration.preferredGender === "FEMALE"
                                                  ? "gender-female"
                                                  : "gender-unset"
                                        }
                                    />
                                    {t("settings.gender")}
                                </dt>
                                <dd>
                                    {t(
                                        registration.preferredGender === "MALE"
                                            ? "matchingFlow.male"
                                            : registration.preferredGender === "FEMALE"
                                              ? "matchingFlow.female"
                                              : "matchingFlow.any",
                                    )}
                                </dd>
                            </div>
                            <div>
                                <dt>
                                    <Icon name="profile" />
                                    {t("matchingFlow.ageSummary")}
                                </dt>
                                <dd>
                                    {registration.preferredMinAge === null &&
                                    registration.preferredMaxAge === null
                                        ? t("matchingFlow.anyAge")
                                        : `${registration.preferredMinAge === null ? t("matchingFlow.any") : t("matchingFlow.ageValue", { count: registration.preferredMinAge })} ~ ${registration.preferredMaxAge === null ? t("matchingFlow.any") : t("matchingFlow.ageValue", { count: registration.preferredMaxAge })}`}
                                </dd>
                            </div>
                        </>
                    )}
                </dl>
                <RecruitmentStyleDetails candidate={candidate} registration={registration} />
            </div>
            <footer className="matching-details-panel__footer">
                {registration && requestAction}
                {onViewResults && (
                    <Button key={postId} variant="highlight" onClick={onViewResults}>
                        {t("roamMatePreview.viewResults")}
                    </Button>
                )}
                <div className="matching-details-panel__actions">
                    {candidate && requestAction}
                    {registration && status === "ACTIVE" && onEdit && (
                        <Button
                            variant="edit"
                            disabled={busy || !registration.destinationId}
                            onClick={onEdit}
                        >
                            {t("matchingFlow.edit")}
                        </Button>
                    )}
                    <Button variant="close" onClick={onClose}>
                        {t("common.close")}
                    </Button>
                </div>
                {failed && (
                    <p className="matching-error" role="alert">
                        {errorMessage ?? t("matchingFlow.saveError")}
                    </p>
                )}
            </footer>
        </DetailsSidebar>
    );
}
