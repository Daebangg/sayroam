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
    | "conversation"
    | "compass"
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
    | "journey-list"
    | "style-plan-essential"
    | "style-plan-usual"
    | "style-plan-flexible"
    | "style-plan-spontaneous"
    | "style-plan-free"
    | "style-pace-slow"
    | "style-pace-relaxed"
    | "style-pace-balanced"
    | "style-pace-active"
    | "style-pace-packed"
    | "style-time-independent"
    | "style-time-apart"
    | "style-time-balanced"
    | "style-time-mostly"
    | "style-time-together"
    | "style-budget-save"
    | "style-budget-careful"
    | "style-budget-moderate"
    | "style-budget-worthwhile"
    | "style-budget-experiences";

type IconProps = {
    name: IconName;
    className?: string;
    variant?: "brand" | "outline";
};

export function Icon({ name, className = "", variant = "brand" }: IconProps) {
    if (name.startsWith("style-") && variant === "brand") {
        return (
            <img
                src={`/brand/travel-style/${name}.webp`}
                className={`icon icon--illustration ${className}`}
                width={24}
                height={24}
                alt=""
                aria-hidden="true"
                decoding="async"
            />
        );
    }
    const sprite = name.startsWith("style-") ? "/brand/travel-style-icons.svg" : "/brand/icons.svg";
    const symbol = variant === "outline" ? `${name}-outline` : name;
    return (
        <svg className={`icon ${className}`} viewBox="0 0 24 24" aria-hidden="true">
            <use href={`${sprite}#${symbol}`} />
        </svg>
    );
}
