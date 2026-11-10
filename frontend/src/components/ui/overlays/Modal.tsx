import { useEffect, useRef, type ReactNode } from "react";

type ModalProps = {
    open: boolean;
    onClose: () => void;
    labelledBy: string;
    children: ReactNode;
    className?: string;
    dismissible?: boolean;
};

export function Modal({
    open,
    onClose,
    labelledBy,
    children,
    className = "",
    dismissible = true,
}: ModalProps) {
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
            data-modal-active={open || undefined}
            onCancel={(event) => {
                event.preventDefault();
                if (dismissible) onClose();
            }}
            onClick={(event) => {
                const rect = event.currentTarget.getBoundingClientRect();
                if (
                    dismissible &&
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
