import { useId } from "react";
import { useTranslation } from "react-i18next";
import { Button, DropdownMenu, Icon, Modal } from "@/components/ui";
import type { ThemePreference } from "@/hooks/useTheme";

type AppearancePreviewProps = {
    open: boolean;
    onClose: () => void;
    theme: ThemePreference;
    onThemeChange: (preference: ThemePreference) => void;
    onLanguageChange: () => void;
};

export function AppearancePreview({
    open,
    onClose,
    theme,
    onThemeChange,
    onLanguageChange,
}: AppearancePreviewProps) {
    const { t, i18n } = useTranslation();
    const id = useId();
    return (
        <Modal open={open} onClose={onClose} labelledBy={id} className="ui-modal--showcase">
            <Button
                variant="ghost"
                className="icon-button ui-modal__close"
                onClick={onClose}
                aria-label={t("common.close")}
            >
                <Icon name="close" />
            </Button>
            <h2 id={id}>{t("profile.settings")}</h2>
            <div className="appearance-settings">
                <div>
                    <h3>{t("profile.theme")}</h3>
                    <div className="component-example__row">
                        {(["system", "light", "dark"] as const).map((option) => (
                            <Button
                                key={option}
                                variant={theme === option ? "soft" : "outline"}
                                aria-pressed={theme === option}
                                onClick={() => onThemeChange(option)}
                            >
                                {option !== "system" && (
                                    <Icon name={option === "light" ? "sun" : "moon"} />
                                )}
                                {t(`profile.${option}`)}
                            </Button>
                        ))}
                    </div>
                </div>
                <div>
                    <h3>{t("profile.language")}</h3>
                    <DropdownMenu
                        label={t("profile.language")}
                        value={i18n.resolvedLanguage ?? "ko"}
                        options={[
                            { value: "ko", label: t("profile.korean") },
                            { value: "en", label: t("profile.english") },
                        ]}
                        onChange={(language) => {
                            if (language !== i18n.resolvedLanguage) onLanguageChange();
                        }}
                    />
                </div>
            </div>
        </Modal>
    );
}
