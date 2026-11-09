import { Icon, type IconName } from "../icons/Icon";

type BadgeProps = {
    label: string;
    tone?: "brand" | "warm" | "success" | "neutral";
    icon?: IconName;
};

export function Badge({ label, tone = "brand", icon }: BadgeProps) {
    return (
        <span className={`ui-badge ui-badge--${tone}`}>
            {icon && <Icon name={icon} variant={tone === "success" ? "outline" : "brand"} />}
            {label}
        </span>
    );
}

export function NotificationBadge({ count, label }: { count: number; label: string }) {
    if (count <= 0) return null;
    return (
        <span className="ui-notification-badge" aria-label={label}>
            {count}
        </span>
    );
}
