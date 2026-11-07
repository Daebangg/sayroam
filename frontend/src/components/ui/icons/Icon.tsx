export type IconName =
    | "home"
    | "trips"
    | "matching"
    | "rooms"
    | "profile"
    | "settings"
    | "grid"
    | "bell"
    | "search"
    | "bookmark"
    | "plus"
    | "arrow"
    | "chevron"
    | "close"
    | "sun"
    | "moon"
    | "mail"
    | "filter";

type IconProps = {
    name: IconName;
    className?: string;
};

export function Icon({ name, className = "" }: IconProps) {
    return (
        <svg className={`icon ${className}`} viewBox="0 0 24 24" aria-hidden="true">
            <use href={`/brand/icons.svg#${name}`} />
        </svg>
    );
}
