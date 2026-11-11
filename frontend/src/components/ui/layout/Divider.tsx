type DividerProps = { className?: string };

export function Divider({ className = "" }: DividerProps) {
    return <hr className={`ui-divider ${className}`} aria-hidden="true" />;
}
