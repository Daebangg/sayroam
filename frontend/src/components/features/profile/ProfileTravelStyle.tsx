import { useTranslation } from "react-i18next";
import { Button, Icon, Spinner, Toast, Tooltip } from "@/components/ui";
import { SettingsRow } from "@/components/features/settings/SettingsContent";
import { profileStyleQuestions, type ProfileStyleValues } from "@/data/profileStyle";

type ProfileTravelStyleProps = {
    values: ProfileStyleValues | null;
    failed: boolean;
    saved: boolean;
    onEdit: () => void;
    onRetry: () => void;
    onDismissSaved: () => void;
};
const icons = {
    planning: "journey-plan",
    pace: "journey-pace",
    together: "journey-together",
    spending: "journey-budget",
} as const;
export function ProfileTravelStyle({
    values,
    failed,
    saved,
    onEdit,
    onRetry,
    onDismissSaved,
}: ProfileTravelStyleProps) {
    const { t } = useTranslation();
    return (
        <>
            <SettingsRow label={t("profileSetup.styleTitle")} className="profile-travel-style">
                {values === null && !failed && <Spinner label={t("matchingFlow.loading")} />}
                {values !== null && (
                    <>
                        <dl className="profile-travel-style__summary">
                            {profileStyleQuestions.map((question) => {
                                const level = ([1, 2, 3, 4, 5] as const).find(
                                    (value) => value === values[question],
                                );
                                return (
                                    <div key={question}>
                                        <dt className="visually-hidden">
                                            {t(`matchingFlow.styleLabels.${question}`)}
                                        </dt>
                                        <dd>
                                            <Tooltip
                                                content={t(`matchingFlow.styleLabels.${question}`)}
                                            >
                                                <span className="profile-travel-style__value">
                                                    <Icon name={icons[question]} />
                                                    {level
                                                        ? t(
                                                              `matchingFlow.styleDegrees.${question}.${level}`,
                                                          )
                                                        : t("matchingFlow.profileMissing")}
                                                </span>
                                            </Tooltip>
                                        </dd>
                                    </div>
                                );
                            })}
                        </dl>
                        <Tooltip content={t("profileSetup.editStyle")}>
                            <Button
                                variant="plain"
                                className="settings-edit-button"
                                aria-label={t("profileSetup.editStyle")}
                                onClick={onEdit}
                            >
                                <Icon name="edit" />
                            </Button>
                        </Tooltip>
                    </>
                )}
                {failed && (
                    <div className="profile-travel-style__error" role="alert">
                        <span>{t("profileSetup.styleError")}</span>
                        <Button variant="plain" onClick={onRetry}>
                            {t("profileSetup.retry")}
                        </Button>
                    </div>
                )}
            </SettingsRow>
            {saved && <Toast message={t("profileSetup.styleSaved")} onDismiss={onDismissSaved} />}
        </>
    );
}
