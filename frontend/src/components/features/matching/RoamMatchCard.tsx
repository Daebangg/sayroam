import { useTranslation } from "react-i18next";
import { Avatar, Badge, Button, Card, Icon } from "@/components/ui";
import type { CandidateResult } from "@/hooks/matching.types";
import { profileStyleCodes, profileStyleQuestions } from "@/data/profileStyle";
import { formatDateRange } from "@/i18n/locale";

type RoamMatchCardProps = {
    candidate: CandidateResult["candidates"][number];
    destination: string;
    selected: boolean;
    onSelect: () => void;
};

export function RoamMatchCard({ candidate, destination, selected, onSelect }: RoamMatchCardProps) {
    const { t, i18n } = useTranslation();
    const { age, gender, nickname, travelStyleMatch: style } = candidate;
    const language = i18n.resolvedLanguage ?? "ko";
    const similar = style?.category === "SIMILAR";

    const discussionTopics = profileStyleQuestions
        .filter((key) =>
            style?.items.some(
                (item) =>
                    item.code === profileStyleCodes[key] &&
                    (item.similarity === "DIFFERENT" || item.similarity === "SOMEWHAT_DIFFERENT"),
            ),
        )
        .map((key) => t(`matchingFlow.styleLabels.${key}`));
    const styleLabel = !style
        ? t("matchingFlow.styleNoData")
        : similar
          ? t("roamMatePreview.found.similar")
          : discussionTopics[0]
            ? t(
                  discussionTopics.length > 1
                      ? "matchingFlow.discussMultiple"
                      : "matchingFlow.discussOne",
                  { topic: discussionTopics[0], count: discussionTopics.length - 1 },
              )
            : t("roamMatePreview.found.discussion");

    const ageLabel =
        age === null ? t("matchingFlow.private") : t("matchingFlow.ageValue", { count: age });
    const genderLabel = gender ? t(`settings.genderValues.${gender}`) : t("matchingFlow.private");

    return (
        <Card className={`roam-match-card${selected ? " is-selected" : ""}`}>
            <header className="roam-match-card__person">
                <Avatar src={candidate.profileImageUrl} />
                <div>
                    <strong>{nickname}</strong>
                    <small className="roam-match-card__profile">
                        <span title={t("matchingFlow.myAge")}>
                            <Icon name="journey-age" />
                            <span className="visually-hidden">{t("matchingFlow.myAge")} </span>
                            {ageLabel}
                        </span>
                        <span title={genderLabel}>
                            <Icon
                                name={
                                    gender === "FEMALE"
                                        ? "gender-female"
                                        : gender === "MALE"
                                          ? "gender-male"
                                          : "gender-unset"
                                }
                            />
                            <span className="visually-hidden">{t("settings.gender")} </span>
                            {genderLabel}
                        </span>
                    </small>
                </div>
                <Badge
                    label={styleLabel}
                    tone={!style ? "neutral" : similar ? "success" : "warm"}
                />
            </header>
            <h2>
                <Button
                    variant="plain"
                    className="matching-registration__open"
                    aria-expanded={selected}
                    aria-controls={selected ? "recruitment-details-panel" : undefined}
                    onClick={onSelect}
                >
                    {candidate.title || t("matchingFlow.untitled")}
                </Button>
            </h2>
            {candidate.introduction && <p>{candidate.introduction}</p>}
            <dl className="roam-match-card__facts">
                <div>
                    <dt>
                        <Icon name="pin" />
                        {t("roamMatePreview.found.region")}
                    </dt>
                    <dd>{destination}</dd>
                </div>
                <div>
                    <dt>
                        <Icon name="calendar" />
                        {t("matchingFlow.period")}
                    </dt>
                    <dd>{formatDateRange(language, candidate.startDate, candidate.endDate)}</dd>
                </div>
                <div>
                    <dt>
                        <Icon name="journey-together" />
                        {t("matchingFlow.overlap", { count: candidate.overlapDays })}
                    </dt>
                    <dd>
                        {formatDateRange(
                            language,
                            candidate.overlapStartDate,
                            candidate.overlapEndDate,
                        )}
                    </dd>
                </div>
            </dl>
        </Card>
    );
}
