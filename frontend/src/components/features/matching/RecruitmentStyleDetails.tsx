import { useTranslation } from "react-i18next";
import { Icon, Tag } from "@/components/ui";
import type { CompanionRequest, MatchingRegistration } from "@/hooks/matching.types";

export function RecruitmentStyleDetails({
    registration,
    candidate,
}: {
    registration?: MatchingRegistration | undefined;
    candidate?: CompanionRequest["candidate"] | undefined;
}) {
    const { t, i18n } = useTranslation();
    return (
        <>
            {candidate && (
                <p className="matching-details-style-summary">
                    {candidate.travelStyleMatch ? (
                        <>
                            <strong>{t("profileSetup.styleTitle")}</strong>
                            <span title={t("matchingFlow.styleMatchLabel")}>
                                <Tag
                                    tone="primary"
                                    label={t("matchingFlow.styleMatchScore", {
                                        score: new Intl.NumberFormat(i18n.resolvedLanguage, {
                                            style: "percent",
                                            maximumFractionDigits: 2,
                                        }).format(candidate.travelStyleMatch.score / 100),
                                    })}
                                />
                            </span>
                        </>
                    ) : (
                        t("matchingFlow.styleUnavailable")
                    )}
                </p>
            )}
            {(registration || candidate?.travelStyleMatch) && (
                <dl className="matching-conditions-summary matching-details-panel__styles">
                    {(
                        [
                            ["planning", "planning", "journey-plan"],
                            ["pace", "daily-pace", "journey-pace"],
                            ["together", "time-together", "journey-together"],
                            ["spending", "spending", "journey-budget"],
                        ] as const
                    ).map(([name, code, icon]) => {
                        const item = candidate?.travelStyleMatch?.items.find(
                            (entry) => entry.code === code,
                        );
                        const score = registration
                            ? registration.travelStyle[code]
                            : item?.candidateScore;
                        const level = ([1, 2, 3, 4, 5] as const).find((value) => value === score);
                        const myLevel = ([1, 2, 3, 4, 5] as const).find(
                            (value) => value === item?.myScore,
                        );
                        const discuss =
                            item?.similarity === "SOMEWHAT_DIFFERENT" ||
                            item?.similarity === "DIFFERENT";
                        return (
                            <div
                                key={code}
                                className={discuss ? "matching-style-needs-discussion" : undefined}
                            >
                                <dt>
                                    <Icon name={icon} />
                                    {t(`matchingFlow.styleLabels.${name}`)}
                                </dt>
                                <dd>
                                    <div className="matching-details-style-value">
                                        <span>
                                            {level
                                                ? t(`matchingFlow.styleDegrees.${name}.${level}`)
                                                : t("matchingFlow.profileMissing")}
                                        </span>
                                        {item && (
                                            <small>
                                                {t(
                                                    `matchingFlow.styleSimilarity.${item.similarity}`,
                                                )}
                                            </small>
                                        )}
                                    </div>
                                    {item && myLevel && (
                                        <small className="matching-details-comparison">
                                            {t("matchingFlow.styleMine", {
                                                value: t(
                                                    `matchingFlow.styleDegrees.${name}.${myLevel}`,
                                                ),
                                            })}
                                        </small>
                                    )}
                                </dd>
                            </div>
                        );
                    })}
                </dl>
            )}
        </>
    );
}
