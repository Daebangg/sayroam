import { Avatar } from "./Avatar";
import type { AvatarPreset } from "./avatarPresets";
import { Button } from "../buttons/Button";

type AvatarPickerProps = {
    label: string;
    value: AvatarPreset | null;
    options: readonly { value: AvatarPreset; label: string }[];
    onChange: (value: AvatarPreset) => void;
    disabled?: boolean;
};

export function AvatarPicker({
    label,
    value,
    options,
    onChange,
    disabled = false,
}: AvatarPickerProps) {
    return (
        <div className="ui-avatar-picker" role="group" aria-label={label}>
            {options.map((option) => (
                <Button
                    key={option.value}
                    variant="plain"
                    className="ui-avatar-picker__option"
                    aria-label={option.label}
                    aria-pressed={value === option.value}
                    disabled={disabled}
                    onClick={() => onChange(option.value)}
                >
                    <span className="ui-avatar-picker__portrait">
                        <Avatar preset={option.value} size="large" />
                    </span>
                    <span>{option.label}</span>
                </Button>
            ))}
        </div>
    );
}
