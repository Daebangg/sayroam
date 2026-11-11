import type { ThemePreference } from "../../../hooks/theme.types";
import { Button } from "../buttons/Button";
import { TravelArtwork } from "../brand/TravelArtwork";

type ThemePickerProps = {
    label: string;
    value: ThemePreference;
    options: readonly { value: ThemePreference; label: string }[];
    onChange: (value: ThemePreference) => void;
    disabled?: boolean;
};

export function ThemePicker({
    label,
    value,
    options,
    onChange,
    disabled = false,
}: ThemePickerProps) {
    return (
        <div className="ui-theme-picker__options" role="group" aria-label={label}>
            {options.map((option) => (
                <Button
                    key={option.value}
                    variant="plain"
                    className="ui-theme-picker__option"
                    aria-pressed={value === option.value}
                    disabled={disabled}
                    onClick={() => onChange(option.value)}
                >
                    <span
                        className={`ui-theme-picker__preview ui-theme-picker__preview--${option.value}`}
                        aria-hidden="true"
                    >
                        <span className="ui-theme-picker__sidebar">
                            <i />
                            <i />
                            <i />
                        </span>
                        <TravelArtwork scene="coast" />
                        <span className="ui-theme-picker__lines">
                            <i />
                            <i />
                            <i />
                        </span>
                    </span>
                    <span className="ui-theme-picker__label">{option.label}</span>
                </Button>
            ))}
        </div>
    );
}
