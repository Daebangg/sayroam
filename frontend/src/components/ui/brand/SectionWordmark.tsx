import { useTranslation } from "react-i18next";

const labels = {
    settings: "settings.signature",
    profile: "settings.profileSignature",
    matching: "matchingFlow.signature",
    "travel-style": "profileSetup.styleTitle",
} as const;

type SectionWordmarkProps = { variant: keyof typeof labels };

export function SectionWordmark({ variant }: SectionWordmarkProps) {
    const { t } = useTranslation();
    return (
        <span className={`section-wordmark section-wordmark--${variant}`}>
            <img
                className="section-wordmark__light"
                src={`/brand/${variant}-wordmark.svg`}
                alt={t(labels[variant])}
            />
            <img
                className="section-wordmark__dark"
                src={`/brand/${variant}-wordmark-light.svg`}
                alt={t(labels[variant])}
            />
        </span>
    );
}
