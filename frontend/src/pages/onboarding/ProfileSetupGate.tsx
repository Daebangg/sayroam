import { useBlocker, useNavigate } from "react-router";
import { ProfileGreetingPage } from "./ProfileGreetingPage";
import { useEffect, useId, useState, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    DatePicker,
    IconChoiceGroup,
    Spinner,
    Tooltip,
    Icon,
    Modal,
} from "@/components/ui";
import { TravelStyleChoices } from "@/components/features/matching/TravelStyleChoices";
import { useTravelStyle } from "@/hooks/api/profile/useTravelStyle";
import { useProfileEditor } from "@/hooks/api/auth/useProfileEditor";
import {
    hasCompleteStyle,
    profileStyleQuestions,
    type ProfileStyleValues,
} from "@/data/profileStyle";
import type { PageContext } from "@/routing/context";
import { calendarToday, formatCalendarDate } from "@/components/ui/inputs/calendar";

type ProfileSetupGateProps = {
    member: NonNullable<PageContext["member"]>;
    onUpdate: PageContext["onUpdateProfileDetails"];
    children: ReactNode;
    onActivity: (active: boolean) => void;
};
export function ProfileSetupGate({
    member,
    onUpdate,
    children,
    onActivity,
}: ProfileSetupGateProps) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const [welcoming, setWelcoming] = useState(false);
    const style = useTravelStyle(member.userId);
    const details = useProfileEditor(onUpdate);
    const storageKey = `sayroam.profile-setup.dismissed.${member.userId}`;
    const [dismissed, setDismissed] = useState(() => {
        try {
            return localStorage.getItem(storageKey) === "1";
        } catch {
            return false;
        }
    });
    function dismiss(): void {
        try {
            localStorage.setItem(storageKey, "1");
        } catch {
            /* Continue for this session if storage is unavailable. */
        }
        setDismissed(true);
    }
    const [step, setStep] = useState(0);
    const [gender, setGender] = useState(member.gender);
    const [birthDate, setBirthDate] = useState(member.birthDate ?? "");
    const [draft, setDraft] = useState<ProfileStyleValues>({});
    const [saving, setSaving] = useState(false);
    const values = { ...style.values, ...draft };
    const today = formatCalendarDate(calendarToday());
    const age = birthDate
        ? Number(today.slice(0, 4)) -
          Number(birthDate.slice(0, 4)) -
          (today.slice(5) < birthDate.slice(5) ? 1 : 0)
        : null;
    const active = !dismissed;
    useEffect(() => {
        try {
            localStorage.setItem(storageKey, dismissed ? "1" : "0");
        } catch {
            // The current session can continue when browser storage is unavailable.
        }
    }, [storageKey, dismissed]);
    const blocker = useBlocker(
        ({ currentLocation, nextLocation }) =>
            active &&
            (nextLocation.pathname === "/" || currentLocation.pathname !== nextLocation.pathname),
    );
    const leaveTitle = useId();
    useEffect(() => {
        onActivity(active || welcoming);
        return () => onActivity(false);
    }, [active, welcoming, onActivity]);
    const question = profileStyleQuestions[step - 2];
    const busy = saving || details.busy || style.busy;
    const canContinue =
        step === 0
            ? gender !== null
            : step === 1
              ? Boolean(birthDate)
              : Boolean(question && values[question]);
    async function finish(): Promise<void> {
        if (busy || !gender || !birthDate || !hasCompleteStyle(values)) return;
        setSaving(true);
        try {
            const input: Parameters<typeof details.save>[0] = {};
            if (member.gender !== gender) input.gender = gender;
            if (member.birthDate !== birthDate) input.birthDate = birthDate;
            if (Object.keys(input).length && !(await details.save(input))) return;
            if (await style.save(values)) {
                dismiss();
                setWelcoming(true);
            }
        } finally {
            setSaving(false);
        }
    }
    if (welcoming)
        return (
            <ProfileGreetingPage
                onStart={() => {
                    setWelcoming(false);
                    void navigate("/", { replace: true });
                }}
            />
        );
    if (dismissed) return children;
    if (style.values === null && !style.failed)
        return (
            <main id="main-content" tabIndex={-1} className="profile-onboarding">
                <Spinner label={t("matchingFlow.loading")} />
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
                {style.values === null && style.failed ? (
                    <div role="alert">
                        <p>{t("profileSetup.styleError")}</p>
                        <Button onClick={style.reload}>{t("profileSetup.retry")}</Button>
                    </div>
                ) : (
                    <fieldset className="profile-onboarding__input" disabled={busy}>
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
                {(details.message || (style.failed && style.values !== null)) && (
                    <p role="alert" className="profile-travel-style__error">
                        {details.message
                            ? t(`settings.detailsMessages.${details.message}`)
                            : t("profileSetup.styleError")}
                    </p>
                )}
            </section>
            <Tooltip
                className="profile-onboarding__nav profile-onboarding__nav--previous"
                content={t("matchingFlow.previous")}
            >
                <Button
                    variant="plain"
                    className="matching-step-arrow"
                    aria-label={t("matchingFlow.previous")}
                    disabled={busy || step === 0}
                    onClick={() => setStep(step - 1)}
                >
                    <Icon name="arrow" variant="brand" className="matching-back-arrow" />
                </Button>
            </Tooltip>
            <Tooltip
                className="profile-onboarding__nav profile-onboarding__nav--next"
                content={t(
                    !canContinue || style.values === null
                        ? "profileSetup.later"
                        : step === 5
                          ? "profileSetup.finish"
                          : "matchingFlow.next",
                )}
            >
                <Button
                    variant="plain"
                    className="matching-step-arrow"
                    aria-label={t(
                        !canContinue || style.values === null
                            ? "profileSetup.later"
                            : step === 5
                              ? "profileSetup.finish"
                              : "matchingFlow.next",
                    )}
                    disabled={busy}
                    aria-busy={saving || undefined}
                    onClick={() => {
                        if (!canContinue || style.values === null) dismiss();
                        else if (step === 5) void finish();
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
            <Modal
                open={blocker.state === "blocked"}
                labelledBy={leaveTitle}
                onClose={() => blocker.reset?.()}
                className="matching-end-dialog"
            >
                <h2 id={leaveTitle}>{t("profileSetup.leaveTitle")}</h2>
                <p className="matching-end-dialog__title">{t("profileSetup.leaveHint")}</p>
                <div className="matching-end-dialog__actions">
                    <Button variant="outline" onClick={() => blocker.reset?.()}>
                        {t("common.cancel")}
                    </Button>
                    <Button
                        variant="brand"
                        disabled={busy}
                        onClick={() => {
                            dismiss();
                            blocker.proceed?.();
                        }}
                    >
                        {t("profileSetup.leave")}
                    </Button>
                </div>
            </Modal>
        </main>
    );
}
