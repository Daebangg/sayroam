import { useTranslation } from "react-i18next";

export function Brand() {
    const { t } = useTranslation();

    return (
        <span className="brand">
            <img className="brand__light" src="/brand/sayroam-logo.svg" alt={t("app.name")} />
            <img className="brand__dark" src="/brand/sayroam-logo-light.svg" alt={t("app.name")} />
        </span>
    );
}
