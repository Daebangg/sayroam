import { useId, useRef, useState, type KeyboardEvent } from "react";
import { TextField } from "./TextField";
import { Icon } from "../icons/Icon";
import { MenuItem } from "../dropdowns/MenuItem";

type Option = { value: string; label: string };
type AutocompleteProps = {
    label: string;
    value: string;
    query: string;
    options: readonly Option[];
    placeholder?: string;
    emptyLabel: string;
    disabled?: boolean;
    onQueryChange: (query: string) => void;
    onSelect: (option: Option) => void;
};

export function Autocomplete({
    label,
    value,
    query,
    options,
    placeholder,
    emptyLabel,
    disabled,
    onQueryChange,
    onSelect,
}: AutocompleteProps) {
    const id = useId();
    const listId = `${id}-options`;
    const [open, setOpen] = useState(false);
    const [active, setActive] = useState(-1);
    const root = useRef<HTMLDivElement>(null);
    const selected = options.find((option) => option.value === value);
    const filtered = options.filter(
        (option) =>
            !query.trim() ||
            option.label.toLocaleLowerCase().includes(query.trim().toLocaleLowerCase()),
    );
    const visible = open && !disabled;
    function select(option: Option): void {
        onSelect(option);
        setOpen(false);
        setActive(-1);
    }
    function handleKey(event: KeyboardEvent<HTMLInputElement>): void {
        if (event.nativeEvent.isComposing) return;
        if (event.key === "Escape") {
            if (visible) event.preventDefault();
            setOpen(false);
            return;
        }
        if (event.key === "ArrowDown" || event.key === "ArrowUp") {
            event.preventDefault();
            setOpen(true);
            const next =
                filtered.length === 0
                    ? -1
                    : event.key === "ArrowDown"
                      ? (active + 1) % filtered.length
                      : active <= 0
                        ? filtered.length - 1
                        : active - 1;
            setActive(next);
            requestAnimationFrame(() =>
                root.current
                    ?.querySelector(`#${CSS.escape(`${id}-option-${next}`)}`)
                    ?.scrollIntoView({ block: "nearest" }),
            );
        }
        if (event.key === "Enter" && visible) {
            event.preventDefault();
            const option = filtered[active] ?? (filtered.length === 1 ? filtered[0] : undefined);
            if (option) select(option);
        }
    }
    return (
        <div className="ui-autocomplete" ref={root}>
            <TextField
                label={label}
                role="combobox"
                autoComplete="off"
                value={selected?.label ?? query}
                placeholder={placeholder}
                disabled={disabled}
                aria-expanded={visible}
                aria-controls={visible ? listId : undefined}
                aria-autocomplete="list"
                aria-activedescendant={
                    visible && active >= 0 && filtered[active]
                        ? `${id}-option-${active}`
                        : undefined
                }
                onFocus={() => setOpen(true)}
                onBlur={() => {
                    setOpen(false);
                    setActive(-1);
                }}
                onChange={(event) => {
                    onQueryChange(event.target.value);
                    setOpen(true);
                    setActive(-1);
                }}
                onKeyDown={handleKey}
            />
            <Icon
                name={selected ? "check" : "search"}
                variant="outline"
                className="ui-autocomplete__icon"
            />
            {visible && (
                <div className="ui-autocomplete__panel ui-menu-surface">
                    <div
                        id={listId}
                        role="listbox"
                        aria-label={label}
                        className="ui-dropdown__scroll"
                    >
                        {filtered.map((option, index) => (
                            <MenuItem
                                key={option.value}
                                id={`${id}-option-${index}`}
                                role="option"
                                aria-selected={option.value === value}
                                tabIndex={-1}
                                data-active={active === index || undefined}
                                onMouseDown={(event) => event.preventDefault()}
                                onClick={() => select(option)}
                            >
                                {option.label}
                                {option.value === value && (
                                    <Icon
                                        name="check"
                                        variant="outline"
                                        className="ui-dropdown__selected"
                                    />
                                )}
                            </MenuItem>
                        ))}
                    </div>
                    {filtered.length === 0 && (
                        <p role="status" className="ui-autocomplete__empty">
                            {emptyLabel}
                        </p>
                    )}
                </div>
            )}
        </div>
    );
}
