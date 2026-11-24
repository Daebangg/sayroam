import { useId } from "react";
import { useBlocker, type BlockerFunction } from "react-router";
import { Button, Modal } from "@/components/ui";

type PageLeaveDialogProps = {
    shouldBlock: BlockerFunction;
    title: string;
    description: string;
    stayLabel: string;
    leaveLabel: string;
    leaveDisabled?: boolean;
    onLeave?: () => void;
};

export function PageLeaveDialog({
    shouldBlock,
    title,
    description,
    stayLabel,
    leaveLabel,
    leaveDisabled,
    onLeave,
}: PageLeaveDialogProps) {
    const blocker = useBlocker(shouldBlock);
    const titleId = useId();
    const stay = () => blocker.reset?.();
    return (
        <Modal
            open={blocker.state === "blocked"}
            labelledBy={titleId}
            onClose={stay}
            className="matching-end-dialog"
        >
            <h2 id={titleId}>{title}</h2>
            <p className="matching-end-dialog__title">{description}</p>
            <div className="matching-end-dialog__actions">
                <Button variant="cancel" onClick={stay}>
                    {stayLabel}
                </Button>
                <Button
                    variant="confirm"
                    disabled={leaveDisabled}
                    onClick={() => {
                        onLeave?.();
                        blocker.proceed?.();
                    }}
                >
                    {leaveLabel}
                </Button>
            </div>
        </Modal>
    );
}
