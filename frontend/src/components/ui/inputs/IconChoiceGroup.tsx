import { Button } from "../buttons/Button";
import { Icon, type IconName } from "../icons/Icon";
import { Tooltip } from "../overlays/Tooltip";

type IconChoiceGroupProps<T extends string> = {
    label: string;
    value: T;
    options: readonly { value: T; label: string; icon: IconName }[];
    onChange: (value: T) => void;
    disabled?: boolean;
};

export function IconChoiceGroup<T extends string>({
    label,
    value,
    options,
    onChange,
    disabled = false,
}: IconChoiceGroupProps<T>) {
    return (
        <div className="ui-icon-choice" role="group" aria-label={label}>
            {options.map((option) => (
                <Tooltip key={option.value} content={option.label}>
                    <Button
                        variant="plain"
                        className="ui-icon-choice__option"
                        aria-label={option.label}
                        aria-pressed={value === option.value}
                        disabled={disabled}
                        onClick={() => onChange(option.value)}
                    >
                        <Icon name={option.icon} />
                    </Button>
                </Tooltip>
            ))}
        </div>
    );
}
