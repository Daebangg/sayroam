import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, Modal, TravelArtwork } from "@/components/ui";

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
                <Button
                    variant="ghost"
                    className="icon-button"
                    aria-label={t("common.close")}
                    onClick={onClose}
                >
                    <Icon name="close" variant="outline" />
                </Button>
            </div>
            <TravelArtwork scene={scene} />
            <h2 id={id}>{title}</h2>
            <p className="ui-modal__description">{description}</p>
            <Button variant="brand" onClick={onClose}>
                {t("mockup.confirm")}
            </Button>
        </Modal>
    );
}
