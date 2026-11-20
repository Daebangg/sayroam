import { useEffect, useRef } from "react";
import { useTranslation } from "react-i18next";
import { Button, Icon } from "@/components/ui";

type ProfileGreetingPageProps = { onStart: () => void };

export function ProfileGreetingPage({ onStart }: ProfileGreetingPageProps) {
    const { t } = useTranslation();
    const heading = useRef<HTMLHeadingElement>(null);
    useEffect(() => {
        heading.current?.focus({ preventScroll: true });
    }, []);
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className="status-page status-page--illustrated profile-greeting"
        >
            <section className="profile-greeting__content" aria-labelledby="profile-greeting-title">
                <img
                    className="profile-greeting__landscape"
                    src="/images/travel-coast.jpg"
                    alt=""
                    aria-hidden="true"
                />
                <h1 id="profile-greeting-title" ref={heading} tabIndex={-1}>
                    {t("profileSetup.greetingTitle")}
                </h1>
                <p>{t("profileSetup.greetingDescription")}</p>
                <Button variant="brand" onClick={onStart}>
                    {t("profileSetup.greetingStart")}
                    <Icon name="arrow" variant="outline" />
                </Button>
            </section>
        </main>
    );
}
