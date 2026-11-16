import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal } from "@/components/ui";

type EndRecruitmentDialogProps = {
    title: string;
    busy: boolean;
    failed: boolean;
    onCancel: () => void;
    onConfirm: () => void;
};

export function EndRecruitmentDialog({
    title,
    busy,
    failed,
    onCancel,
    onConfirm,
}: EndRecruitmentDialogProps) {
    const { t } = useTranslation();
    const id = useId();
    return (
        <Modal
            open
            labelledBy={id}
            onClose={onCancel}
            dismissible={!busy}
            className="matching-end-dialog"
        >
            <h2 id={id}>{t("matchingFlow.endConfirmation")}</h2>
            <p className="matching-end-dialog__title">{title}</p>
            {failed && (
                <p role="alert" className="matching-error">
                    {t("matchingFlow.endFailed")}
                </p>
            )}
            <div className="matching-end-dialog__actions">
                <Button variant="outline" disabled={busy} onClick={onCancel}>
                    {t("common.cancel")}
                </Button>
                <Button variant="brand" loading={busy} onClick={onConfirm}>
                    {t("matchingFlow.close")}
                </Button>
            </div>
        </Modal>
    );
}
