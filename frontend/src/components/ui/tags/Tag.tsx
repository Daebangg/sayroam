import { useTranslation } from "react-i18next";
import { Icon } from "../icons/Icon";

type TagProps = {
    label: string;
    tone?: "neutral" | "primary" | "success";
    onRemove?: () => void;
};

export function Tag({ label, tone = "neutral", onRemove }: TagProps) {
    const { t } = useTranslation();
    return (
        <span className={`ui-tag ui-tag--${tone}`}>
            <span>{label}</span>
            {onRemove && (
                <button
                    type="button"
                    onClick={onRemove}
                    aria-label={t("common.removeTag", { name: label })}
                >
                    <Icon name="close" />
                </button>
            )}
        </span>
    );
}
