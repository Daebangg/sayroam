import { useEffect, useRef } from "react";
import { useNavigate, useLocation } from "react-router";
import { useTranslation } from "react-i18next";
import { Brand, ButtonLink, DropdownMenu, Icon } from "@/components/ui";
import { ProfileSetupPage } from "@/pages/onboarding/ProfileSetupPage";
import { HomePage } from "@/pages/home/HomePage";
import { TripsPage } from "@/pages/trips/TripsPage";
import { MatchingPreview } from "./MatchingPreview";
import { RoomsPage } from "@/pages/rooms/RoomsPage";
import { RoamMatePreview } from "./RoamMatePreview";
import { mockupScreens, mockupUrl, type MockupScreen } from "@/routing/mockups";

export function MockupPage({ screen }: { screen: MockupScreen }) {
    const { t } = useTranslation();
    const navigate = useNavigate();
    const location = useLocation();
    const preparingEntry = useRef<string | undefined>(undefined);
    const discovery =
        screen === "matching-searching" ||
        screen === "matching-found" ||
        screen === "matching-empty" ||
        screen === "matching-delayed";
    const needsParent = discovery && !location.state?.fromMatchingMain;

    // 직접 열거나 알림에서 들어온 목업도 브라우저 뒤로가기의 목적지가 롬 메이트 메인이 되게 한다.
    useEffect(() => {
        if (!needsParent || preparingEntry.current === location.key) return;
        preparingEntry.current = location.key;
        async function enterFromMain() {
            await navigate(mockupUrl("matching"), { replace: true });
            await navigate(
                { pathname: location.pathname, search: location.search, hash: location.hash },
                { state: { fromMatchingMain: true } },
            );
        }
        void enterFromMain();
    }, [needsParent, location, navigate]);
    if (needsParent) return null;
    const intro = screen === "welcome" || screen === "onboarding";
    return (
        <>
            <aside className="mockup-toolbar" aria-label={t("mockup.screen")}>
                <DropdownMenu
                    label={t("mockup.screen")}
                    value={screen}
                    options={mockupScreens.map((value) => ({
                        value,
                        label: t(`mockup.screens.${value}`),
                    }))}
                    onChange={(value) => {
                        const next = mockupScreens.find((item) => item === value);
                        if (next)
                            void navigate(mockupUrl(next), {
                                state:
                                    screen === "matching" && !location.state?.matchingView
                                        ? { fromMatchingMain: true }
                                        : undefined,
                            });
                    }}
                />
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
            ) : screen === "profile-setup" ? (
                <ProfileSetupPage
                    member={{ nickname: "Roam", gender: null, birthDate: null }}
                    savedStyle={{}}
                    homePath={mockupUrl("home")}
                />
            ) : screen === "notifications" ? (
                <RoamMatePreview key={screen} screen={screen} />
            ) : screen === "home" ? (
                <HomePage />
            ) : screen === "trips" ? (
                <TripsPage />
            ) : screen === "matching" || discovery ? (
                <MatchingPreview screen={screen} />
            ) : (
                <RoomsPage />
            )}
        </>
    );
}
