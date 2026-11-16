export type IconName =
    | "home"
    | "trips"
    | "matching"
    | "rooms"
    | "profile"
    | "settings"
    | "keyboard"
    | "gender-male"
    | "gender-female"
    | "gender-unset"
    | "edit"
    | "grid"
    | "bell"
    | "search"
    | "bookmark"
    | "plus"
    | "minus"
    | "arrow"
    | "chevron"
    | "close"
    | "sun"
    | "moon"
    | "mail"
    | "filter"
    | "pin"
    | "guide"
    | "check"
    | "flame"
    | "plan"
    | "calendar"
    | "journey-place"
    | "journey-dates"
    | "journey-people"
    | "journey-age"
    | "journey-plan"
    | "journey-pace"
    | "journey-together"
    | "journey-budget"
    | "journey-register"
    | "journey-list";

type IconProps = {
    name: IconName;
    className?: string;
    variant?: "brand" | "outline";
};

export function Icon({ name, className = "", variant = "brand" }: IconProps) {
    const symbol = variant === "outline" ? `${name}-outline` : name;
    return (
        <svg className={`icon ${className}`} viewBox="0 0 24 24" aria-hidden="true">
            <use href={`/brand/icons.svg#${symbol}`} />
        </svg>
    );
}
