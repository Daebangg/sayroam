import { Icon, type IconName } from "../icons/Icon";

type BadgeProps = {
    label: string;
    className?: string;
    tone?: "brand" | "warm" | "success" | "neutral" | "danger";
    icon?: IconName;
};

export function Badge({ label, tone = "brand", icon, className = "" }: BadgeProps) {
    return (
        <span className={`ui-badge ui-badge--${tone} ${className}`}>
            {icon && (
                <Icon
                    name={icon}
                    variant={tone === "success" || tone === "danger" ? "outline" : "brand"}
                />
            )}
            {label}
        </span>
    );
}

export function NotificationBadge({
    count,
    label,
    maxCount,
}: {
    count: number;
    label: string;
    maxCount?: number;
}) {
    if (count <= 0) return null;
    return (
        <span className="ui-notification-badge" aria-label={label}>
            {maxCount !== undefined && count > maxCount ? `${maxCount}+` : count}
        </span>
    );
}
