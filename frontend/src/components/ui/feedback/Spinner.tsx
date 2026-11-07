type SpinnerProps = { label: string; labelHidden?: boolean };

export function Spinner({ label, labelHidden = false }: SpinnerProps) {
    return (
        <span className="ui-loading" role="status">
            <span className="ui-spinner ui-spinner--standalone" aria-hidden="true" />
            <span className={labelHidden ? "visually-hidden" : undefined}>{label}</span>
        </span>
    );
}
