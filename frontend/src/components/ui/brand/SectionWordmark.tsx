import { useTranslation } from "react-i18next";

type SectionWordmarkProps = { variant: "settings" | "profile" };

export function SectionWordmark({ variant }: SectionWordmarkProps) {
    const { t } = useTranslation();
    return (
        <span className={`section-wordmark section-wordmark--${variant}`}>
            <img
                className="section-wordmark__light"
                src={`/brand/${variant}-wordmark.svg`}
                alt={t(variant === "profile" ? "settings.profileSignature" : "settings.signature")}
            />
            <img
                className="section-wordmark__dark"
                src={`/brand/${variant}-wordmark-light.svg`}
                alt={t(variant === "profile" ? "settings.profileSignature" : "settings.signature")}
            />
        </span>
    );
}
