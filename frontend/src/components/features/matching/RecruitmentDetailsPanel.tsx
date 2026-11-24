import { RecruitmentTextAction } from "./RecruitmentTextAction";
import { useEffect, useEffectEvent, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Avatar, Button, Icon, Tag, Tooltip } from "@/components/ui";
import { RecruitmentStatusBadge } from "./RecruitmentStatusBadge";
import type { CandidateResult, MatchingRegistration } from "@/hooks/matching.types";

type RecruitmentDetailsPanelProps = {
    destination: string;
    onClose: () => void;
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
    | { candidate: CandidateResult["candidates"][number]; registration?: never }
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
    const panel = useRef<HTMLElement>(null);
    const content = useRef<HTMLDivElement>(null);
    const close = useEffectEvent(onClose);
    useEffect(() => {
        const opener = document.activeElement;
        const media = window.matchMedia("(max-width: 700px)");
        const page = document.getElementById("main-content");
        const previousOverflow = document.body.style.overflow;
        function update(): void {
            if (page) page.inert = media.matches;
            document.body.style.overflow = media.matches ? "hidden" : previousOverflow;
        }
        update();
        panel.current?.focus({ preventScroll: true });
        function keydown(event: KeyboardEvent): void {
            if (event.defaultPrevented || document.querySelector("dialog[open]")) return;
            if (
                event.target instanceof Element &&
                event.target.closest(".site-header, .chat-sidebar")
            )
                return;
            if (event.key === "Escape" && !event.isComposing) {
                event.preventDefault();
                close();
                return;
            }
        }
        function outside(event: PointerEvent): void {
            const target = event.target;
            if (
                !(target instanceof Element) ||
                panel.current?.contains(target) ||
                document.querySelector("dialog[open]")
            )
                return;
            // Let another recruitment card handle selection without closing its new panel.
            if (
                target.closest(
                    ".matching-registration__open, .matching-registration, .roam-match-card, .ui-dropdown__menu, .chat-sidebar, [aria-keyshortcuts='Alt+C']",
                )
            )
                return;
            close();
        }
        document.addEventListener("pointerdown", outside);
        media.addEventListener("change", update);
        document.addEventListener("keydown", keydown);
        return () => {
            document.removeEventListener("pointerdown", outside);
            media.removeEventListener("change", update);
            document.removeEventListener("keydown", keydown);
            if (page) page.inert = false;
            document.body.style.overflow = previousOverflow;
            if (opener instanceof HTMLElement && opener.isConnected)
                opener.focus({ preventScroll: true });
        };
    }, []);
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
    return createPortal(
        <aside
            ref={panel}
            id="recruitment-details-panel"
            tabIndex={-1}
            role="region"
            aria-labelledby={titleId}
            className={`edge-panel matching-details-panel${closing ? " matching-details-panel--closing" : ""}`}
            onAnimationEnd={(event) => {
                if (
                    closing &&
                    event.target === event.currentTarget &&
                    event.animationName === "matching-panel-exit"
                )
                    onExited();
            }}
        >
            <div ref={content} className="matching-details-panel__body">
                <header className="matching-details-panel__header">
                    <div>
                        {registration && (
                            <div className="matching-details-panel__status">
                                <RecruitmentStatusBadge
                                    status={onViewResults ? "MATCHED" : registration.status}
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
                        <h2 id={titleId} aria-live="polite">
                            {post.title || t("matchingFlow.untitled")}
                        </h2>
                        {post.introduction && (
                            <div className="matching-details-introduction">
                                <p>{post.introduction}</p>
                            </div>
                        )}
                    </div>
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
                {candidate && (
                    <p className="matching-details-style-summary">
                        {candidate.travelStyleMatch ? (
                            <>
                                <strong>{t("profileSetup.styleTitle")}</strong>
                                <span title={t("matchingFlow.styleMatchLabel")}>
                                    <Tag
                                        tone="primary"
                                        label={t("matchingFlow.styleMatchScore", {
                                            score: new Intl.NumberFormat(i18n.resolvedLanguage, {
                                                style: "percent",
                                                maximumFractionDigits: 2,
                                            }).format(candidate.travelStyleMatch.score / 100),
                                        })}
                                    />
                                </span>
                            </>
                        ) : (
                            t("matchingFlow.styleUnavailable")
                        )}
                    </p>
                )}
                {(registration || candidate.travelStyleMatch) && (
                    <dl className="matching-conditions-summary matching-details-panel__styles">
                        {(
                            [
                                ["planning", "planning", "journey-plan"],
                                ["pace", "daily-pace", "journey-pace"],
                                ["together", "time-together", "journey-together"],
                                ["spending", "spending", "journey-budget"],
                            ] as const
                        ).map(([name, code, icon]) => {
                            const item = candidate?.travelStyleMatch?.items.find(
                                (entry) => entry.code === code,
                            );
                            const score = registration
                                ? registration.travelStyle[code]
                                : item?.candidateScore;
                            const level = ([1, 2, 3, 4, 5] as const).find(
                                (value) => value === score,
                            );
                            const myLevel = ([1, 2, 3, 4, 5] as const).find(
                                (value) => value === item?.myScore,
                            );
                            const discuss =
                                item?.similarity === "SOMEWHAT_DIFFERENT" ||
                                item?.similarity === "DIFFERENT";
                            return (
                                <div
                                    key={code}
                                    className={
                                        discuss ? "matching-style-needs-discussion" : undefined
                                    }
                                >
                                    <dt>
                                        <Icon name={icon} />
                                        {t(`matchingFlow.styleLabels.${name}`)}
                                    </dt>
                                    <dd>
                                        <div className="matching-details-style-value">
                                            <span>
                                                {level
                                                    ? t(
                                                          `matchingFlow.styleDegrees.${name}.${level}`,
                                                      )
                                                    : t("matchingFlow.profileMissing")}
                                            </span>
                                            {item && (
                                                <small>
                                                    {t(
                                                        `matchingFlow.styleSimilarity.${item.similarity}`,
                                                    )}
                                                </small>
                                            )}
                                        </div>
                                        {item && myLevel && (
                                            <small className="matching-details-comparison">
                                                {t("matchingFlow.styleMine", {
                                                    value: t(
                                                        `matchingFlow.styleDegrees.${name}.${myLevel}`,
                                                    ),
                                                })}
                                            </small>
                                        )}
                                    </dd>
                                </div>
                            );
                        })}
                    </dl>
                )}
            </div>
            <footer className="matching-details-panel__footer">
                {onViewResults && (
                    <Button key={postId} variant="highlight" onClick={onViewResults}>
                        {t("roamMatePreview.openMatches")}
                    </Button>
                )}
                <div className="matching-details-panel__actions">
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
        </aside>,
        document.querySelector(".app-shell") ?? document.body,
    );
}
