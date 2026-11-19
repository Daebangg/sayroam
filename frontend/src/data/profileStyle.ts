export const profileStyleQuestions = ["planning", "pace", "together", "spending"] as const;
export const profileStyleCodes = {
    planning: "planning",
    pace: "daily-pace",
    together: "time-together",
    spending: "spending",
} as const;
export type ProfileStyleQuestion = (typeof profileStyleQuestions)[number];
export type ProfileStyleValues = Partial<Record<ProfileStyleQuestion, number>>;
export function hasCompleteStyle(values: ProfileStyleValues): boolean {
    return profileStyleQuestions.every((key) => {
        const score = values[key];
        return score !== undefined && Number.isInteger(score) && score >= 1 && score <= 5;
    });
}
