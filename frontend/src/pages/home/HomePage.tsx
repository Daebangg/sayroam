import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";

type HomePageProps = {
    onStart: () => void;
    onOpenBrand: () => void;
};

export function HomePage({ onStart, onOpenBrand }: HomePageProps) {
    const { t } = useTranslation();

    return (
        <main className="home-page">
            <img
                className="home-page__landscape"
                src="/images/alpine-dawn.png"
                alt=""
                fetchPriority="high"
            />
            <div className="home-page__wash" />
            <div className="home-page__stars" />
            <div className="home-page__content">
                <h1>
                    {t("home.titleFirst")}
                    <br />
                    {t("home.titleSecond")}
                </h1>
                <span className="home-page__underline" aria-hidden="true" />
                <p>
                    {t("home.descriptionFirst")}
                    <br />
                    {t("home.descriptionSecond")}
                </p>
                <Button className="home-page__start" onClick={onStart}>
                    {t("home.start")}
                    <Icon name="arrow" />
                </Button>
            </div>
            <button type="button" className="brand-guide-link" onClick={onOpenBrand}>
                {t("brand.guide")}
                <Icon name="arrow" />
            </button>
        </main>
    );
}
