import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Modal, ModalCloseButton, TravelArtwork } from "@/components/ui";

type PreviewDialogProps = {
    open: boolean;
    title: string;
    description: string;
    scene?: "coast" | "city" | "mountain" | "island";
    onClose: () => void;
};
export function PreviewDialog({
    open,
    title,
    description,
    scene = "mountain",
    onClose,
}: PreviewDialogProps) {
    const id = useId();
    const { t } = useTranslation();
    return (
        <Modal open={open} onClose={onClose} labelledBy={id} className="preview-dialog">
            <div className="preview-dialog__toolbar">
                <ModalCloseButton inline onClose={onClose} />
            </div>
            <TravelArtwork scene={scene} />
            <h2 id={id}>{title}</h2>
            <p className="ui-modal__description">{description}</p>
            <Button variant="confirm" onClick={onClose}>
                {t("mockup.confirm")}
            </Button>
        </Modal>
    );
}
