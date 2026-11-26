import { useTranslation } from "react-i18next";
import { Button, Icon, Tooltip, type IconName } from "@/components/ui";
import type { Candidate } from "@/hooks/matching.types";

type CandidateRequestActionProps = {
    candidate: Pick<
        Candidate,
        "searchId" | "canRequest" | "requestId" | "requestStatus" | "direction"
    >;
    placement: "card" | "details";
    busyId?: number | undefined;
    actionsDisabled?: boolean;
    onRespond?: (requestId: number) => void;
    onReject?: (requestId: number) => void;
    onApply?: (id: number) => void;
    recheck?: (() => void) | undefined;
};

const statusIcons = {
    PENDING: "journey-pace",
    ACCEPTED: "check",
    REJECTED: "close",
    EXPIRED: "calendar",
} as const satisfies Record<NonNullable<Candidate["requestStatus"]>, IconName>;

export function CandidateRequestAction({
    candidate,
    placement,
    busyId,
    actionsDisabled = false,
    onApply,
    onRespond,
    onReject,
    recheck,
}: CandidateRequestActionProps) {
    const { t } = useTranslation();
    if (recheck)
        return (
            <div className="companion-request-actions">
                <span role="alert">{t("companionRequests.uncertain")}</span>
                <Button variant="soft" onClick={recheck}>
                    {t("companionRequests.recheck")}
                </Button>
            </div>
        );
    const { requestStatus, direction, requestId } = candidate;
    const className = placement === "card" ? "matching-card-action" : undefined;
    if (requestStatus === "PENDING" && direction === "RECEIVED" && requestId)
        return (
            <div className="companion-request-actions">
                <Button
                    variant="brand"
                    className={className}
                    data-request-action="respond"
                    onClick={() => onRespond?.(requestId)}
                    disabled={!onRespond || busyId !== undefined || actionsDisabled}
                >
                    {placement === "card" && <Icon name="mail" />}
                    {t("companionRequests.respond")}
                </Button>
                {placement === "details" && onReject && (
                    <Button
                        variant="delete"
                        onClick={() => onReject(requestId)}
                        disabled={busyId !== undefined || actionsDisabled}
                    >
                        {t("companionRequests.reject")}
                    </Button>
                )}
            </div>
        );
    if (!requestStatus && (!candidate.canRequest || !onApply)) return null;
    const label =
        requestStatus === "PENDING"
            ? direction === "SENT"
                ? "sent"
                : "incoming"
            : (requestStatus ?? "send");
    const button = (
        <Button
            variant={
                placement === "details" && requestStatus === "ACCEPTED"
                    ? "edit"
                    : placement === "details" && requestStatus === "REJECTED"
                      ? "delete"
                      : placement === "details" && requestStatus === "EXPIRED"
                        ? "cancel"
                        : "brand"
            }
            className={className}
            data-request-status={requestStatus ?? undefined}
            onClick={() => onApply?.(candidate.searchId)}
            disabled={!!requestStatus || busyId !== undefined || actionsDisabled}
            loading={!requestStatus && busyId === candidate.searchId}
            aria-live="polite"
        >
            {placement === "card" && (
                <Icon name={requestStatus ? statusIcons[requestStatus] : "mail"} />
            )}
            {t(`companionRequests.${label}`)}
            {placement === "card" && !requestStatus && <Icon name="arrow" variant="outline" />}
        </Button>
    );
    if (!requestStatus) return button;
    const hint =
        requestStatus === "PENDING"
            ? direction === "SENT"
                ? "sent"
                : "incoming"
            : requestStatus === "REJECTED"
              ? direction === "RECEIVED"
                  ? "REJECTED_RECEIVED"
                  : "REJECTED_SENT"
              : requestStatus;
    return (
        <Tooltip
            touchHint
            className="candidate-request-status"
            content={t(`companionRequests.hints.${hint}`)}
        >
            <span tabIndex={0} role="group" aria-label={t(`companionRequests.${label}`)}>
                {button}
            </span>
        </Tooltip>
    );
}
