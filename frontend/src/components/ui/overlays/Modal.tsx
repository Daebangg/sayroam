import { useEffect, useRef, type ReactNode } from "react";

type ModalProps = {
    open: boolean;
    onClose: () => void;
    labelledBy: string;
    children: ReactNode;
    className?: string;
};

export function Modal({ open, onClose, labelledBy, children, className = "" }: ModalProps) {
    const ref = useRef<HTMLDialogElement>(null);

    useEffect(() => {
        const dialog = ref.current;
        if (open && !dialog?.open) dialog?.showModal();
        if (!open && dialog?.open) dialog.close();
    }, [open]);

    return (
        <dialog
            ref={ref}
            className={`ui-modal ${className}`}
            aria-labelledby={labelledBy}
            onCancel={onClose}
            onClick={(event) => {
                const rect = event.currentTarget.getBoundingClientRect();
                if (
                    event.target === event.currentTarget &&
                    (event.clientX < rect.left ||
                        event.clientX > rect.right ||
                        event.clientY < rect.top ||
                        event.clientY > rect.bottom)
                )
                    onClose();
            }}
        >
            {children}
        </dialog>
    );
}
