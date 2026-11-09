type KeyboardShortcutProps = {
    keys: readonly string[];
};

export function KeyboardShortcut({ keys }: KeyboardShortcutProps) {
    const mac = typeof navigator !== "undefined" && /Mac|iPhone|iPad/.test(navigator.platform);
    const labels: Record<string, string> = {
        mod: mac ? "⌘" : "Ctrl",
        shift: mac ? "⇧" : "Shift",
        alt: mac ? "⌥" : "Alt",
        escape: "Esc",
    };

    return (
        <kbd className="ui-shortcut">
            {keys.map((key, index) => (
                <span key={`${key}-${index}`} className="ui-shortcut__key">
                    {labels[key.toLowerCase()] ?? key.toUpperCase()}
                </span>
            ))}
        </kbd>
    );
}
