import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal } from "@/components/ui";
import type { RequestDecision, CompanionRequest } from "@/hooks/matching.types";

type CompanionResponseDialogProps = {
    status: RequestDecision;
    candidate: CompanionRequest["candidate"];
    busy: boolean;
    onCancel: () => void;
    onConfirm: () => void;
};

export function CompanionResponseDialog({
    status,
    candidate,
    busy,
    onCancel,
    onConfirm,
}: CompanionResponseDialogProps) {
    const titleId = useId();
    const { t } = useTranslation();
    return (
        <Modal
            open
            labelledBy={titleId}
            className="matching-end-dialog"
            dismissible={!busy}
            onClose={onCancel}
        >
            <h2 id={titleId}>
                {t(
                    status === "ACCEPTED"
                        ? "companionRequests.confirmAccept"
                        : "companionRequests.confirmReject",
                )}
            </h2>
            {candidate && (
                <p className="matching-end-dialog__title">
                    {candidate.nickname}
                    {candidate.title && ` · ${candidate.title}`}
                </p>
            )}
            <div className="matching-end-dialog__actions">
                <Button variant="cancel" disabled={busy} onClick={onCancel}>
                    {t("common.cancel")}
                </Button>
                <Button
                    variant={status === "ACCEPTED" ? "brand" : "delete"}
                    data-request-action={status === "ACCEPTED" ? "respond" : undefined}
                    loading={busy}
                    onClick={onConfirm}
                >
                    {t(
                        status === "ACCEPTED"
                            ? "companionRequests.accept"
                            : "companionRequests.reject",
                    )}
                </Button>
            </div>
        </Modal>
    );
}
