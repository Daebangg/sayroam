import { useNavigate } from "react-router";
import { PageLeaveDialog } from "@/components/layout/PageLeaveDialog";
import { ProfileGreetingPage } from "./ProfileGreetingPage";
import { useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { Button, DatePicker, IconChoiceGroup, Spinner, Tooltip, Icon } from "@/components/ui";
import { TravelStyleChoices } from "@/components/features/matching/TravelStyleChoices";
import {
    hasCompleteStyle,
    profileStyleQuestions,
    type ProfileStyleValues,
} from "@/data/profileStyle";
import type { PageContext } from "@/routing/context";
import { calendarToday, formatCalendarDate } from "@/components/ui/inputs/calendar";

type SetupMember = Pick<NonNullable<PageContext["member"]>, "nickname" | "gender" | "birthDate">;
type ProfileSetupPageProps = {
    member: SetupMember;
    savedStyle: ProfileStyleValues | null;
    styleFailed?: boolean;
    onReloadStyle?: () => void;
    busy?: boolean;
    error?: ReactNode;
    visitError?: ReactNode;
    homePath: string;
    onLeave?: () => void;
    onSave?: (
        gender: NonNullable<SetupMember["gender"]>,
        birthDate: string,
        values: ProfileStyleValues,
    ) => Promise<boolean>;
};

export function ProfileSetupPage({
    member,
    savedStyle,
    styleFailed,
    onReloadStyle,
    busy = false,
    error,
    visitError,
    homePath,
    onLeave,
    onSave,
}: ProfileSetupPageProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [welcoming, setWelcoming] = useState(false);
    const [step, setStep] = useState(0);
    const [gender, setGender] = useState(member.gender);
    const [birthDate, setBirthDate] = useState(member.birthDate ?? "");
    const [draft, setDraft] = useState<ProfileStyleValues>({});
    const [saving, setSaving] = useState(false);
    const values = { ...savedStyle, ...draft };
    const today = formatCalendarDate(calendarToday());
    const age = birthDate
        ? Number(today.slice(0, 4)) -
          Number(birthDate.slice(0, 4)) -
          (today.slice(5) < birthDate.slice(5) ? 1 : 0)
        : null;
    const question = profileStyleQuestions[step - 2];
    const disabled = saving || busy;
    const canContinue =
        step === 0
            ? gender !== null
            : step === 1
              ? Boolean(birthDate)
              : Boolean(question && values[question]);
    async function finish(): Promise<void> {
        if (disabled || !gender || !birthDate || !hasCompleteStyle(values)) return;
        setSaving(true);
        try {
            if (!onSave || (await onSave(gender, birthDate, values))) setWelcoming(true);
        } finally {
            setSaving(false);
        }
    }
    if (welcoming)
        return (
            <ProfileGreetingPage
                onStart={() => {
                    onLeave?.();
                    void navigate(homePath, { replace: true });
                }}
            />
        );
    const leaveDialog = (
        <PageLeaveDialog
            shouldBlock={({ currentLocation, nextLocation }) =>
                nextLocation.pathname === "/" ||
                nextLocation.pathname + nextLocation.search === homePath ||
                currentLocation.pathname !== nextLocation.pathname
            }
            title={t("profileSetup.leaveTitle")}
            description={t("profileSetup.leaveHint")}
            stayLabel={t("common.cancel")}
            leaveLabel={t("common.confirm")}
            leaveDisabled={disabled}
            onLeave={() => onLeave?.()}
        />
    );
    if (savedStyle === null && !styleFailed)
        return (
            <main id="main-content" tabIndex={-1} className="profile-onboarding">
                <Spinner label={t("matchingFlow.loading")} />
                {leaveDialog}
            </main>
        );
    return (
        <main id="main-content" tabIndex={-1} className="profile-onboarding">
            <div className="settings-landscape" aria-hidden="true" />
            <section className="profile-onboarding__content" aria-labelledby="profile-setup-title">
                <p className="profile-onboarding__eyebrow">
                    {t("profileSetup.welcome", { nickname: member.nickname })}
                </p>
                <h1 id="profile-setup-title">
                    {t(
                        step === 0
                            ? "profileSetup.genderQuestion"
                            : step === 1
                              ? "profileSetup.birthdayQuestion"
                              : `matchingFlow.questions.${question!}`,
                    )}
                </h1>
                <p className="profile-travel-style__hint">{t("profileSetup.intro")}</p>
                {visitError}
                {savedStyle === null && styleFailed ? (
                    <div role="alert">
                        <p>{t("profileSetup.styleError")}</p>
                        <Button onClick={onReloadStyle}>{t("profileSetup.retry")}</Button>
                    </div>
                ) : (
                    <fieldset className="profile-onboarding__input" disabled={disabled}>
                        {step === 0 && (
                            <IconChoiceGroup
                                showLabels
                                label={t("settings.gender")}
                                value={gender ?? "unset"}
                                options={[
                                    {
                                        value: "MALE",
                                        label: t("settings.genderValues.MALE"),
                                        icon: "gender-male",
                                    },
                                    {
                                        value: "FEMALE",
                                        label: t("settings.genderValues.FEMALE"),
                                        icon: "gender-female",
                                    },
                                ]}
                                onChange={(value) =>
                                    setGender(value === "MALE" || value === "FEMALE" ? value : null)
                                }
                            />
                        )}
                        {step === 1 && (
                            <div className="profile-onboarding__birthday">
                                <DatePicker
                                    yearNavigation
                                    label={t("settings.birthday")}
                                    value={birthDate}
                                    min={`${calendarToday().getUTCFullYear() - 100}-01-01`}
                                    max={formatCalendarDate(calendarToday())}
                                    onChange={setBirthDate}
                                    placeholder={t("profileSetup.birthdayPlaceholder")}
                                />
                                {age !== null && (
                                    <p className="profile-onboarding__age" role="status">
                                        {t("profileSetup.age", { count: age })}
                                    </p>
                                )}
                            </div>
                        )}
                        {question && (
                            <TravelStyleChoices
                                question={question}
                                value={values[question] ?? 0}
                                onChange={(value) => setDraft({ ...draft, [question]: value })}
                            />
                        )}
                    </fieldset>
                )}
                {error && (
                    <p role="alert" className="profile-travel-style__error">
                        {error}
                    </p>
                )}
                <Button
                    variant="brand"
                    className="step-action-button"
                    disabled={disabled}
                    onClick={() => void navigate(homePath, { replace: true })}
                >
                    <Icon name="check" variant="outline" />
                    {t("profileSetup.later")}
                </Button>
            </section>
            <Tooltip
                className="profile-onboarding__nav profile-onboarding__nav--previous"
                content={t("matchingFlow.previous")}
            >
                <Button
                    variant="plain"
                    className="matching-step-arrow"
                    aria-label={t("matchingFlow.previous")}
                    disabled={disabled || step === 0}
                    onClick={() => setStep(step - 1)}
                >
                    <Icon name="arrow" variant="brand" className="matching-back-arrow" />
                </Button>
            </Tooltip>
            <Tooltip
                className="profile-onboarding__nav profile-onboarding__nav--next"
                content={t(step === 5 ? "profileSetup.finish" : "matchingFlow.next")}
            >
                <Button
                    variant="plain"
                    className="matching-step-arrow"
                    aria-label={t(step === 5 ? "profileSetup.finish" : "matchingFlow.next")}
                    disabled={disabled || !canContinue || savedStyle === null}
                    aria-busy={saving || undefined}
                    onClick={() => {
                        if (step === 5) void finish();
                        else setStep(step + 1);
                    }}
                >
                    {saving ? (
                        <Spinner label={t("matchingFlow.loading")} labelHidden />
                    ) : (
                        <Icon name="arrow" variant="brand" />
                    )}
                </Button>
            </Tooltip>
            {leaveDialog}
        </main>
    );
}
