import { useTranslation } from "react-i18next";
import { Button, Icon, type IconName } from "@/components/ui";

type StyleQuestion = "planning" | "pace" | "together" | "spending";
type TravelStyleChoicesProps = {
    question: StyleQuestion;
    value: number;
    onChange: (value: number) => void;
};
const levels = [1, 2, 3, 4, 5] as const;
const optionIcons = {
    planning: [
        "style-plan-essential",
        "style-plan-usual",
        "style-plan-flexible",
        "style-plan-spontaneous",
        "style-plan-free",
    ],
    pace: [
        "style-pace-slow",
        "style-pace-relaxed",
        "style-pace-balanced",
        "style-pace-active",
        "style-pace-packed",
    ],
    together: [
        "style-time-independent",
        "style-time-apart",
        "style-time-balanced",
        "style-time-mostly",
        "style-time-together",
    ],
    spending: [
        "style-budget-save",
        "style-budget-careful",
        "style-budget-moderate",
        "style-budget-worthwhile",
        "style-budget-experiences",
    ],
} as const satisfies Record<StyleQuestion, readonly IconName[]>;

export function TravelStyleChoices({ question, value, onChange }: TravelStyleChoicesProps) {
    const { t } = useTranslation();
    return (
        <div className="matching-style-choices">
            <div
                className="matching-style-choices__cards"
                role="group"
                aria-label={t(`matchingFlow.styleLabels.${question}`)}
            >
                {levels.map((level, index) => (
                    <Button
                        key={level}
                        variant="plain"
                        className="matching-style-choice"
                        aria-pressed={value === level}
                        onClick={() => onChange(level)}
                    >
                        <Icon
                            name={optionIcons[question][index]!}
                            className="matching-style-choice__illustration"
                        />
                        <span>{t(`matchingFlow.styleDegrees.${question}.${level}`)}</span>
                        <span className="matching-style-choice__check" aria-hidden="true">
                            <Icon name="check" variant="outline" />
                        </span>
                    </Button>
                ))}
            </div>
            <div className="matching-style-choices__scale" aria-hidden="true">
                <span>{t(`matchingFlow.styleDegrees.${question}.1`)}</span>
                <div className="matching-style-choices__dots">
                    {levels.map((level) => (
                        <span key={level} data-selected={value === level || undefined} />
                    ))}
                </div>
                <span>{t(`matchingFlow.styleDegrees.${question}.5`)}</span>
            </div>
        </div>
    );
}
