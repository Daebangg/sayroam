import { useRef, type ReactNode } from "react";
import { Button } from "../buttons/Button";

type FilePickerProps = {
    label: string;
    accept: string;
    disabled?: boolean;
    onSelect: (file: File) => void;
    children?: ReactNode;
    className?: string;
};

export function FilePicker({
    label,
    accept,
    disabled = false,
    onSelect,
    children,
    className = "",
}: FilePickerProps) {
    const input = useRef<HTMLInputElement>(null);
    return (
        <span className="ui-file-picker">
            <input
                ref={input}
                type="file"
                accept={accept}
                disabled={disabled}
                aria-label={label}
                className="ui-file-picker__input"
                tabIndex={-1}
                onChange={(event) => {
                    const file = event.currentTarget.files?.[0];
                    event.currentTarget.value = "";
                    if (file) onSelect(file);
                }}
            />
            {children ? (
                <Button
                    variant="plain"
                    className={className}
                    aria-label={label}
                    disabled={disabled}
                    onClick={() => input.current?.click()}
                >
                    {children}
                </Button>
            ) : (
                <Button variant="soft" disabled={disabled} onClick={() => input.current?.click()}>
                    {label}
                </Button>
            )}
        </span>
    );
}
