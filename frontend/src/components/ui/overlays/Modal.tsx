import { useEffect, useRef, type ReactNode } from "react";

import { useTranslation } from "react-i18next";
import { Button } from "../buttons/Button";
import { Icon } from "../icons/Icon";
import { Tooltip } from "./Tooltip";

type ModalCloseButtonProps = {
    onClose: () => void;
    disabled?: boolean;
    inline?: boolean;
};

export function ModalCloseButton({
    onClose,
    disabled = false,
    inline = false,
}: ModalCloseButtonProps) {
    const { t } = useTranslation();
    const label = t("common.close");
    const className = inline ? "" : "ui-modal__close";
    const button = (
        <Button
            variant="ghost"
            className="icon-button"
            aria-label={label}
            aria-keyshortcuts="Escape"
            disabled={disabled}
            onClick={onClose}
        >
            <Icon name="close" variant="outline" />
        </Button>
    );
    return disabled ? (
        <span className={className}>{button}</span>
    ) : (
        <Tooltip
            content={label}
            shortcut={["Escape"]}
            className={className}
            placement={inline ? "below-end" : "viewport"}
        >
            {button}
        </Tooltip>
    );
}

type ModalProps = {
    open: boolean;
    onClose: () => void;
    labelledBy: string;
    children: ReactNode;
    className?: string;
    dismissible?: boolean;
    closeOnBackdrop?: boolean;
};

export function Modal({
    open,
    onClose,
    labelledBy,
    children,
    className = "",
    dismissible = true,
    closeOnBackdrop = true,
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
                    closeOnBackdrop &&
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
