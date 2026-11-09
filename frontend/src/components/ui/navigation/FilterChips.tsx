import { Button } from "../buttons/Button";

type FilterChipsProps = {
    label: string;
    value: string;
    options: readonly { value: string; label: string; disabled?: boolean }[];
    onChange: (value: string) => void;
};

export function FilterChips({ label, value, options, onChange }: FilterChipsProps) {
    return (
        <div className="ui-filter-chips" role="group" aria-label={label}>
            {options.map((option) => (
                <Button
                    key={option.value}
                    variant="plain"
                    className="ui-filter-chip"
                    aria-pressed={value === option.value}
                    disabled={option.disabled}
                    onClick={() => onChange(option.value)}
                >
                    {option.label}
                </Button>
            ))}
        </div>
    );
}
