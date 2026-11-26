import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal } from "@/components/ui";

export type RecruitmentAction = "edit" | "close" | "resume";

type RecruitmentActionDialogProps = {
    title: string;
    action: RecruitmentAction;
    hasMatches: boolean;
    errorMessage?: string;
    busy: boolean;
    failed: boolean;
    onCancel: () => void;
    onConfirm: () => void;
};

export function RecruitmentActionDialog({
    title,
    action,
    hasMatches,
    errorMessage,
    busy,
    failed,
    onCancel,
    onConfirm,
}: RecruitmentActionDialogProps) {
    const { t } = useTranslation();
    const id = useId();
    const heading = {
        edit: "matchingFlow.editMatchedConfirmation",
        close: "matchingFlow.endConfirmation",
        resume: "matchingFlow.resumeConfirmation",
    } as const;
    const buttons = {
        edit: "matchingFlow.edit",
        close: "matchingFlow.close",
        resume: "matchingFlow.resume",
    } as const;
    const description =
        hasMatches && action !== "resume"
            ? t(
                  action === "edit"
                      ? "matchingFlow.editMatchedWarning"
                      : "matchingFlow.endMatchedWarning",
                  { title },
              )
            : title;
    return (
        <Modal
            open
            labelledBy={id}
            onClose={onCancel}
            dismissible={!busy}
            className="matching-end-dialog"
        >
            <h2 id={id}>{t(heading[action])}</h2>
            <p className="matching-end-dialog__title">{description}</p>
            {failed && (
                <p role="alert" className="matching-error">
                    {errorMessage ?? t("matchingFlow.endFailed")}
                </p>
            )}
            <div className="matching-end-dialog__actions">
                <Button variant="cancel" disabled={busy} onClick={onCancel}>
                    {t("common.cancel")}
                </Button>
                <Button
                    variant={action === "close" ? "delete" : action === "edit" ? "edit" : "confirm"}
                    loading={busy}
                    onClick={onConfirm}
                >
                    {t(buttons[action])}
                </Button>
            </div>
        </Modal>
    );
}
