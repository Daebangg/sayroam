import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon, Tooltip } from "@/components/ui";
import { TravelStyleChoices } from "@/components/features/matching/TravelStyleChoices";
import {
    profileStyleQuestions,
    hasCompleteStyle,
    type ProfileStyleValues,
} from "@/data/profileStyle";

type ProfileStyleEditorProps = {
    initialValues: ProfileStyleValues;
    busy: boolean;
    failed: boolean;
    onSave: (values: ProfileStyleValues) => Promise<boolean>;
    onClose: () => void;
};
export function ProfileStyleEditor({
    initialValues,
    busy,
    failed,
    onSave,
    onClose,
}: ProfileStyleEditorProps) {
    const { t } = useTranslation();
    const [step, setStep] = useState(0);
    const [values, setValues] = useState(initialValues);
    const question = profileStyleQuestions[step]!;
    const last = step === profileStyleQuestions.length - 1;
    async function next(): Promise<void> {
        if (busy || !values[question]) return;
        if (!last) setStep(step + 1);
        else if (hasCompleteStyle(values) && (await onSave(values))) onClose();
    }
    return (
        <section className="profile-style-editor" aria-labelledby="profile-style-editor-title">
            <div className="profile-style-editor__content">
                <h1 id="profile-style-editor-title">{t(`matchingFlow.questions.${question}`)}</h1>
                <p className="profile-travel-style__hint">{t("profileSetup.editHint")}</p>
                <fieldset className="profile-onboarding__input" disabled={busy}>
                    <TravelStyleChoices
                        question={question}
                        value={values[question] ?? 0}
                        onChange={(value) => setValues({ ...values, [question]: value })}
                    />
                </fieldset>
                <div className="profile-style-editor__save">
                    {last && (
                        <Button
                            variant="brand"
                            loading={busy}
                            disabled={busy || !hasCompleteStyle(values)}
                            onClick={() => void next()}
                        >
                            {t("profileSetup.saveStyle")}
                        </Button>
                    )}
                </div>
                {failed && (
                    <p className="profile-travel-style__error" role="alert">
                        {t("profileSetup.styleError")}
                    </p>
                )}
            </div>
            <Tooltip
                className="profile-onboarding__nav profile-onboarding__nav--previous"
                content={t(step === 0 ? "profileSetup.backToProfile" : "matchingFlow.previous")}
            >
                <Button
                    variant="plain"
                    className="matching-step-arrow"
                    aria-label={t(
                        step === 0 ? "profileSetup.backToProfile" : "matchingFlow.previous",
                    )}
                    disabled={busy}
                    onClick={() => (step === 0 ? onClose() : setStep(step - 1))}
                >
                    <Icon name="arrow" className="matching-back-arrow" />
                </Button>
            </Tooltip>
            {!last && (
                <Tooltip
                    className="profile-onboarding__nav profile-onboarding__nav--next"
                    content={t("matchingFlow.next")}
                >
                    <Button
                        variant="plain"
                        className="matching-step-arrow"
                        aria-label={t("matchingFlow.next")}
                        disabled={busy || !values[question]}
                        onClick={() => void next()}
                    >
                        <Icon name="arrow" />
                    </Button>
                </Tooltip>
            )}
        </section>
    );
}
