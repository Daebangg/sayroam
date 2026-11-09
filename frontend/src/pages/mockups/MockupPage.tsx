import { useNavigate } from "react-router";
import { useTranslation } from "react-i18next";
import { Brand, ButtonLink, DropdownMenu, Icon } from "@/components/ui";
import { HomePage } from "@/pages/home/HomePage";
import { TripsPage } from "@/pages/trips/TripsPage";
import { MatchingPage } from "@/pages/matching/MatchingPage";
import { RoomsPage } from "@/pages/rooms/RoomsPage";
import { mockupScreens, mockupUrl, type MockupScreen } from "@/routing/mockups";

export function MockupPage({ screen }: { screen: MockupScreen }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const intro = screen === "welcome" || screen === "onboarding";
    return (
        <>
            <aside className="mockup-toolbar" aria-label={t("mockup.toolbar")}>
                <span>{t("mockup.toolbar")}</span>
                <DropdownMenu
                    label={t("mockup.screen")}
                    value={screen}
                    options={mockupScreens.map((value) => ({
                        value,
                        label: t(`mockup.screens.${value}`),
                    }))}
                    onChange={(value) => {
                        const next = mockupScreens.find((item) => item === value);
                        if (next) void navigate(mockupUrl(next));
                    }}
                />
                <ButtonLink variant="plain" className="auth-text-link" to="/brand">
                    {t("brand.guide")}
                </ButtonLink>
            </aside>
            {intro ? (
                <main
                    id="main-content"
                    tabIndex={-1}
                    className={`intro-mockup intro-mockup--${screen}`}
                >
                    <div className="intro-mockup__art" />
                    <div className="intro-mockup__content">
                        {screen === "welcome" ? (
                            <>
                                <div className="intro-mockup__brand">
                                    <Brand />
                                </div>
                                <h1 className="visually-hidden">{t("app.name")}</h1>
                            </>
                        ) : (
                            <>
                                <h1>
                                    {t("home.titleFirst")}
                                    <br />
                                    {t("home.titleSecond")}
                                </h1>
                                <p>{t("mockup.onboardingDescription")}</p>
                            </>
                        )}
                        <div className="intro-mockup__action">
                            <span className="intro-progress" aria-hidden="true">
                                <i />
                                <i />
                                <i />
                            </span>
                            <ButtonLink
                                variant="primary"
                                to={mockupUrl(screen === "welcome" ? "onboarding" : "home")}
                            >
                                {t(screen === "welcome" ? "mockup.continue" : "home.start")}
                                <Icon name="arrow" variant="outline" />
                            </ButtonLink>
                        </div>
                    </div>
                </main>
            ) : screen === "home" ? (
                <HomePage />
            ) : screen === "trips" ? (
                <TripsPage />
            ) : screen === "matching" ? (
                <MatchingPage />
            ) : (
                <RoomsPage />
            )}
        </>
    );
}
