import { useEffect, useEffectEvent, useId, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { useTranslation } from "react-i18next";
import { Avatar, Badge, Button, Icon } from "@/components/ui";
import type { CandidateResult, MatchingRegistration } from "@/hooks/matching.types";

type RecruitmentDetailsPanelProps = {
    registration: MatchingRegistration;
    destination: string;
    onClose: () => void;
    onEdit?: () => void;
    onFinish?: () => void;
    candidate?: CandidateResult["candidates"][number];
    busy: boolean;
    failed: boolean;
};

export function RecruitmentDetailsPanel({
    registration,
    candidate,
    destination,
    onClose,
    onEdit,
    onFinish,
    busy,
    failed,
}: RecruitmentDetailsPanelProps) {
    const { t, i18n } = useTranslation();
    const titleId = useId();
    const panel = useRef<HTMLElement>(null);
    const content = useRef<HTMLDivElement>(null);
    const [mobile, setMobile] = useState(false);
    const close = useEffectEvent(onClose);
    useEffect(() => {
        const opener = document.activeElement;
        const media = window.matchMedia("(max-width: 700px)");
        const previousOverflow = document.body.style.overflow;
        function update(): void {
            setMobile(media.matches);
            document.body.style.overflow = media.matches ? "hidden" : previousOverflow;
        }
        update();
        panel.current?.focus({ preventScroll: true });
        function keydown(event: KeyboardEvent): void {
            if (event.defaultPrevented || document.querySelector("dialog[open]")) return;
            if (event.key === "Escape") {
                event.preventDefault();
                close();
                return;
            }
            if (!media.matches || event.key !== "Tab") return;
            const items = Array.from(
                panel.current?.querySelectorAll<HTMLElement>(
                    'button:not(:disabled), [href], input:not(:disabled), [tabindex="0"]',
                ) ?? [],
            );
            const first = items[0];
            const last = items[items.length - 1];
            if (!first || !last) {
                event.preventDefault();
                return;
            }
            if (
                event.shiftKey &&
                (document.activeElement === first || document.activeElement === panel.current)
            ) {
                event.preventDefault();
                last.focus();
            } else if (
                !event.shiftKey &&
                (document.activeElement === last || document.activeElement === panel.current)
            ) {
                event.preventDefault();
                first.focus();
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
            if (target.closest(".matching-registration, .ui-dropdown__menu")) return;
            close();
        }
        document.addEventListener("pointerdown", outside);
        media.addEventListener("change", update);
        document.addEventListener("keydown", keydown);
        return () => {
            document.removeEventListener("pointerdown", outside);
            media.removeEventListener("change", update);
            document.removeEventListener("keydown", keydown);
            document.body.style.overflow = previousOverflow;
            if (opener instanceof HTMLElement && opener.isConnected)
                opener.focus({ preventScroll: true });
        };
    }, []);
    useEffect(() => {
        content.current?.scrollTo({ top: 0 });
    }, [registration.id]);
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
            role={mobile ? "dialog" : "region"}
            aria-modal={mobile || undefined}
            aria-labelledby={titleId}
            className="matching-details-panel"
        >
            <div ref={content} className="matching-details-panel__body">
                <header className="matching-details-panel__header">
                    <div>
                        <div className="matching-details-panel__status">
                            <Badge
                                tone={registration.status === "ACTIVE" ? "brand" : "neutral"}
                                label={t(
                                    registration.status === "ACTIVE"
                                        ? "matchingFlow.active"
                                        : "matchingFlow.closed",
                                )}
                            />
                            {registration.status === "ACTIVE" && onFinish && (
                                <Button
                                    variant="plain"
                                    className="matching-text-action"
                                    disabled={busy}
                                    onClick={onFinish}
                                >
                                    {t("matchingFlow.close")}
                                </Button>
                            )}
                        </div>
                        <h2 id={titleId} aria-live="polite">
                            {registration.title || t("matchingFlow.untitled")}
                        </h2>
                    </div>
                </header>
                <dl className="matching-conditions-summary">
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
                            <span>{date(registration.startDate)} ~</span>
                            <span>{date(registration.endDate)}</span>
                        </dd>
                    </div>
                    {candidate ? (
                        <>
                            <div>
                                <dt>
                                    <Icon name="profile" />
                                    {t("matchingFlow.organizer")}
                                </dt>
                                <dd className="companion-search__person">
                                    <Avatar src={candidate.profileImageUrl} />
                                    {candidate.nickname}
                                </dd>
                            </div>
                            <div>
                                <dt>
                                    <Icon name="calendar" />
                                    {t("matchingFlow.overlap", { count: candidate.overlapDays })}
                                </dt>
                                <dd>
                                    {date(candidate.overlapStartDate)} ~{" "}
                                    {date(candidate.overlapEndDate)}
                                </dd>
                            </div>
                        </>
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
            </div>
            <footer className="matching-details-panel__footer">
                <div className="matching-details-panel__actions">
                    {registration.status === "ACTIVE" && onEdit && (
                        <Button
                            variant="brand"
                            disabled={busy || !registration.destinationId}
                            onClick={onEdit}
                        >
                            <Icon name="edit" />
                            {t("matchingFlow.edit")}
                        </Button>
                    )}
                    <Button variant="outline" onClick={onClose}>
                        {t("common.close")}
                    </Button>
                </div>
                {failed && (
                    <p className="matching-error" role="alert">
                        {t("matchingFlow.saveError")}
                    </p>
                )}
            </footer>
        </aside>,
        document.body,
    );
}
