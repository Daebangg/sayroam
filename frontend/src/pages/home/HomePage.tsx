import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";

import { Link, useOutletContext } from "react-router";
import type { PageContext } from "@/routing/context";

export function HomePage() {
    const { onOpenAuth: onStart } = useOutletContext<PageContext>();
    const { t } = useTranslation();

    return (
        <main id="main-content" tabIndex={-1} className="home-page">
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
            <Link className="brand-guide-link" to="/brand">
                {t("brand.guide")}
                <Icon name="arrow" />
            </Link>
        </main>
    );
}
