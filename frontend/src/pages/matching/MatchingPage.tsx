import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useOutletContext } from "react-router";
import {
    Button,
    Card,
    Badge,
    DateRangeCalendar,
    Autocomplete,
    DropdownMenu,
    IconChoiceGroup,
    Checkbox,
    RangeSelector,
    Icon,
    Spinner,
    TextField,
    TextArea,
    Tooltip,
    Toast,
} from "@/components/ui";
import {
    useCompanionMatching,
    type MatchingConditions,
} from "@/hooks/api/matching/useCompanionMatching";
import { EndRecruitmentDialog } from "@/components/features/matching/EndRecruitmentDialog";
import { RecruitmentDetailsPanel } from "@/components/features/matching/RecruitmentDetailsPanel";
import { TravelStyleChoices } from "@/components/features/matching/TravelStyleChoices";
import { MatchingFilterPopover } from "@/components/features/matching/MatchingFilterPopover";
import type { PageContext } from "@/routing/context";

type MatchingDraft = {
    destinationQuery: string;
    startDate: string;
    endDate: string;
    gender: string;
    minAge: string;
    maxAge: string;
    anyAge: boolean;
    view: "entry" | "find";
};

export function MatchingPage() {
    const { t, i18n } = useTranslation();
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const [draft, setDraft] = useState<MatchingDraft>({
        destinationQuery: "",
        startDate: "",
        endDate: "",
        gender: "",
        minAge: "20",
        maxAge: "40",
        anyAge: true,
        view: "entry",
    });
    return (
        <MatchingWorkspace
            key={member?.userId ?? "guest"}
            userId={member?.userId}
            profile={member}
            initialDraft={draft}
            onLogin={(next) => {
                setDraft(next);
                onOpenAuth();
            }}
            language={i18n.resolvedLanguage ?? "ko"}
            title={t("matching.heading")}
        />
    );
}

type MatchingWorkspaceProps = {
    userId: number | undefined;
    profile: PageContext["member"];
    onLogin: (draft: MatchingDraft) => void;
    initialDraft: MatchingDraft;
    language: string;
    title: string;
};

const styleQuestions = ["planning", "pace", "together", "spending"] as const;
const styleCodes = {
    planning: "planning",
    pace: "daily-pace",
    together: "time-together",
    spending: "spending",
} as const;
const questionIcons = [
    "journey-place",
    "journey-dates",
    "profile",
    "journey-people",
    "journey-age",
    "journey-plan",
    "journey-pace",
    "journey-together",
    "journey-budget",
    "journey-register",
] as const;
const progressIcons = [
    "journey-place",
    "journey-dates",
    "profile",
    "journey-people",
    "journey-plan",
    "journey-register",
] as const;

const questionKeys = [
    "destination",
    "dates",
    "privacy",
    "gender",
    "age",
    "planning",
    "pace",
    "together",
    "spending",
    "title",
] as const;

function MatchingWorkspace({
    userId,
    profile,
    onLogin,
    initialDraft,
    language,
    title,
}: MatchingWorkspaceProps) {
    const { t } = useTranslation();
    const today = new Intl.DateTimeFormat("sv-SE", {
        timeZone: "Asia/Seoul",
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
    }).format(new Date());
    const profileAge = profile?.birthDate
        ? Number(today.slice(0, 4)) -
          Number(profile.birthDate.slice(0, 4)) -
          (today.slice(5) < profile.birthDate.slice(5) ? 1 : 0)
        : null;
    const matching = useCompanionMatching(userId);
    const [step, setStep] = useState(0);
    const [view, setView] = useState<string>(initialDraft.view);
    const [destinationId, setDestinationId] = useState("");
    const [destinationQuery, setDestinationQuery] = useState(initialDraft.destinationQuery);
    const [startDate, setStartDate] = useState(initialDraft.startDate);
    const [endDate, setEndDate] = useState(initialDraft.endDate);
    const [registrationTitle, setRegistrationTitle] = useState("");
    const [introduction, setIntroduction] = useState("");
    const [showAge, setShowAge] = useState(false);
    const [showGender, setShowGender] = useState(false);
    const [titleEdited, setTitleEdited] = useState(false);
    const [gender, setGender] = useState(initialDraft.gender);
    const [minAge, setMinAge] = useState(initialDraft.minAge || "20");
    const [anyAge, setAnyAge] = useState(initialDraft.anyAge);
    const [maxAge, setMaxAge] = useState(initialDraft.maxAge || "40");
    const [editingId, setEditingId] = useState<number>();
    const [styles, setStyles] = useState<Record<string, string>>({});
    const [saved, setSaved] = useState(false);
    const [savedEdit, setSavedEdit] = useState(false);
    const [myPage, setMyPage] = useState(0);
    const [animatePanelLayout, setAnimatePanelLayout] = useState(false);
    const [mySearchQuery, setMySearchQuery] = useState("");
    const [myStatusFilter, setMyStatusFilter] = useState("all");
    const [myDestinationFilter, setMyDestinationFilter] = useState("");
    const [myGenderFilter, setMyGenderFilter] = useState("");
    const [myAgeFilter, setMyAgeFilter] = useState(false);
    const [myAgeRange, setMyAgeRange] = useState<[number, number]>([20, 40]);
    const [myStartFilter, setMyStartFilter] = useState("");
    const [myEndFilter, setMyEndFilter] = useState("");
    const filterDate = (value: string) =>
        new Intl.DateTimeFormat(language === "en" ? "en-US" : "ko-KR", {
            month: "long",
            day: "numeric",
            timeZone: "UTC",
        }).format(new Date(`${value}T00:00:00Z`));
    const normalizedMyQuery = mySearchQuery.trim().toLocaleLowerCase();
    const filteredSearches = matching.searches.filter((registration) => {
        const destination = matching.destinations.find(
            (item) => item.id === registration.destinationId,
        );
        const matchesQuery =
            !normalizedMyQuery ||
            [registration.title, destination?.name, registration.legacyRegion].some((value) =>
                value?.toLocaleLowerCase().includes(normalizedMyQuery),
            );

        const matchesAge =
            !myAgeFilter ||
            ((registration.preferredMinAge ?? 19) <= myAgeRange[1] &&
                (registration.preferredMaxAge ?? 100) >= myAgeRange[0]);
        return (
            (!myStartFilter || registration.endDate >= myStartFilter) &&
            (!myEndFilter || registration.startDate <= myEndFilter) &&
            matchesQuery &&
            matchesAge &&
            (!myDestinationFilter || String(registration.destinationId) === myDestinationFilter) &&
            (!myGenderFilter || registration.preferredGender === myGenderFilter) &&
            (myStatusFilter === "all" || registration.status === myStatusFilter)
        );
    });
    const myPageSize = 9;
    const pageCount = Math.max(1, Math.ceil(filteredSearches.length / myPageSize));
    const currentPage = Math.min(myPage, pageCount - 1);
    const visibleSearches = filteredSearches.slice(
        currentPage * myPageSize,
        (currentPage + 1) * myPageSize,
    );
    const [endingId, setEndingId] = useState<number>();
    const [statusAction, setStatusAction] = useState<"close" | "resume">("close");
    const [statusNotice, setStatusNotice] = useState<
        "closedNotice" | "resumedNotice" | "activeLimit" | "travelStyleRequired"
    >();
    const [endFailed, setEndFailed] = useState(false);
    const endingRegistration = matching.searches.find((item) => item.id === endingId);
    const [selectedRegistrationId, setSelectedRegistrationId] = useState<number>();
    const selectedRegistration = matching.searches.find(
        (registration) => registration.id === selectedRegistrationId,
    );
    const selectedDestination = matching.destinations.find(
        (value) => String(value.id) === destinationId,
    );
    const ageValid = (value: string) =>
        value === "" || (/^\d+$/.test(value) && Number(value) >= 19 && Number(value) <= 100);
    const agesValid =
        anyAge ||
        (ageValid(minAge) &&
            ageValid(maxAge) &&
            (!minAge || !maxAge || Number(minAge) <= Number(maxAge)));
    const datesValid = Boolean(startDate && endDate && startDate <= endDate);
    function styleScore(name: (typeof styleQuestions)[number]): number {
        return Number(
            styles[name] ??
                (editingId === undefined
                    ? matching.travelStyle.find((item) => item.code === styleCodes[name])?.score
                    : undefined) ??
                0,
        );
    }
    function styleDescription(name: (typeof styleQuestions)[number], score: number): string {
        const level = ([1, 2, 3, 4, 5] as const).find((value) => value === score);
        return level
            ? t(`matchingFlow.styleDegrees.${name}.${level}`)
            : t("matchingFlow.profileMissing");
    }
    const styleQuestion = styleQuestions[step - 5];
    const stylesValid = styleQuestions.every(
        (name) => styleScore(name) >= 1 && styleScore(name) <= 5,
    );
    const valid = Boolean(
        selectedDestination &&
        datesValid &&
        agesValid &&
        stylesValid &&
        Array.from(introduction).length <= 200,
    );
    const canContinue =
        step === 0
            ? Boolean(selectedDestination)
            : step === 1
              ? datesValid
              : step === 4
                ? agesValid
                : styleQuestion && step >= 5 && step <= 8
                  ? styleScore(styleQuestion) >= 1 && styleScore(styleQuestion) <= 5
                  : true;
    const group =
        step === 0 ? 0 : step === 1 ? 1 : step === 2 ? 2 : step <= 4 ? 3 : step <= 8 ? 4 : 5;
    const date = (value: string) =>
        new Intl.DateTimeFormat(language === "en" ? "en-US" : "ko-KR", {
            month: "short",
            day: "numeric",
            timeZone: "UTC",
        }).format(new Date(`${value}T00:00:00Z`));
    const defaultTitle = t("matchingFlow.defaultTitle", {
        destination: selectedDestination?.name ?? "",
        start: startDate ? date(startDate) : "",
        end: endDate ? date(endDate) : "",
    }).slice(0, 80);
    const displayedTitle = titleEdited ? registrationTitle : defaultTitle;
    const conditions: MatchingConditions = {
        destinationId: Number(destinationId),
        startDate,
        endDate,
        title: displayedTitle.trim() || null,
        introduction: introduction.trim(),
        showAge,
        showGender,
        travelStyle: {
            planning: styleScore("planning"),
            "daily-pace": styleScore("pace"),
            "time-together": styleScore("together"),
            spending: styleScore("spending"),
        },
        preferredGender: gender === "MALE" || gender === "FEMALE" ? gender : null,
        preferredMinAge: !anyAge && minAge ? Number(minAge) : null,
        preferredMaxAge: !anyAge && maxAge ? Number(maxAge) : null,
    };
    function goTo(next: number): void {
        setStep(Math.max(0, Math.min(9, next)));
    }
    function editRegistration(id: number): void {
        const registration = matching.searches.find((item) => item.id === id);
        if (!registration?.destinationId || matching.busy) return;
        setSelectedRegistrationId(undefined);

        setDestinationId(String(registration.destinationId));
        setDestinationQuery("");
        setStartDate(registration.startDate);
        setEndDate(registration.endDate);
        setGender(registration.preferredGender ?? "");
        setAnyAge(registration.preferredMinAge === null && registration.preferredMaxAge === null);
        setMinAge(String(Math.max(19, Math.min(100, registration.preferredMinAge ?? 20))));
        setMaxAge(String(Math.max(19, Math.min(100, registration.preferredMaxAge ?? 40))));
        setRegistrationTitle(registration.title ?? "");
        setIntroduction(registration.introduction ?? "");
        setShowAge(registration.showAge);
        setShowGender(registration.showGender);
        setStyles(
            Object.fromEntries(
                styleQuestions.map((name) => [
                    name,
                    String(registration.travelStyle[styleCodes[name]] ?? 0),
                ]),
            ),
        );
        setTitleEdited(Boolean(registration.title));
        setEditingId(registration.id);
        setSaved(false);
        setView("find");
        goTo(0);
    }
    function requestEnd(id: number, action: "close" | "resume" = "close"): void {
        if (action === "resume" && matching.limitReached) {
            setStatusNotice("activeLimit");
            return;
        }
        if (action === "resume" && !matching.canCreate) return;
        setStatusAction(action);
        setEndFailed(false);
        setEndingId(id);
    }
    async function confirmEnd(): Promise<void> {
        if (endingId === undefined || matching.busy) return;
        const ok =
            statusAction === "resume"
                ? await matching.resume(endingId)
                : await matching.close(endingId);
        if (ok) {
            setEndingId(undefined);
            setStatusNotice(statusAction === "resume" ? "resumedNotice" : "closedNotice");
        } else if (
            matching.getFailureReason() === "matchingFlow.activeLimit" ||
            matching.getFailureReason() === "matchingFlow.travelStyleRequired"
        ) {
            setEndingId(undefined);
            setStatusNotice(
                matching.getFailureReason() === "matchingFlow.activeLimit"
                    ? "activeLimit"
                    : "travelStyleRequired",
            );
        } else setEndFailed(true);
    }
    function login(): void {
        onLogin({
            destinationQuery,
            startDate,
            endDate,
            gender,
            minAge,
            maxAge,
            anyAge,
            view: "find",
        });
    }
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className={`matching-workspace${view === "entry" ? " matching-workspace--entry" : ""}${view === "mine" ? " matching-workspace--mine" : ""}${view === "mine" && animatePanelLayout ? " matching-workspace--panel-transition" : ""}${view === "mine" && selectedRegistration ? " matching-workspace--details" : ""}`}
        >
            <div className="settings-landscape matching-entry__landscape" aria-hidden="true" />
            <div key={view} className="matching-view page-enter">
                {view !== "entry" && (
                    <div className="matching-section-nav">
                        <Button
                            variant="plain"
                            className="settings-back matching-back"
                            disabled={matching.busy}
                            onClick={() => {
                                if (view === "mine") {
                                    setMyPage(0);
                                    setSelectedRegistrationId(undefined);
                                }
                                setView(
                                    view === "find" && editingId !== undefined ? "mine" : "entry",
                                );
                                setEditingId(undefined);
                                setSaved(false);
                            }}
                        >
                            <Icon name="arrow" className="matching-back-arrow" variant="outline" />
                            {t("matchingEntry.back")}
                        </Button>
                    </div>
                )}
                {view === "entry" ? (
                    <section className="matching-entry" aria-label={title}>
                        <header className="matching-heading matching-entry__heading">
                            <h1>
                                {t("matchingEntry.headingBefore")}
                                <span>{t("matchingEntry.headingAccent")}</span>
                                {t("matchingEntry.headingAfter")}
                            </h1>
                            <p>{t("matchingEntry.subtitle")}</p>
                        </header>
                        <div className="matching-entry__choices">
                            <Button
                                variant="plain"
                                className="matching-entry__choice"
                                disabled={
                                    matching.busy ||
                                    (userId !== undefined &&
                                        (matching.catalogLoading || matching.catalogFailed))
                                }
                                onClick={() => {
                                    if (matching.limitReached) {
                                        setStatusNotice("activeLimit");
                                        return;
                                    }
                                    if (userId !== undefined && !matching.canCreate) return;
                                    setEditingId(undefined);
                                    setTitleEdited(false);
                                    setIntroduction("");
                                    setShowAge(false);
                                    setShowGender(false);
                                    setStyles({});
                                    setView("find");
                                    goTo(0);
                                }}
                            >
                                <Icon name="journey-register" />
                                <strong>{t("matchingEntry.registerTitle")}</strong>
                                <span>{t("matchingEntry.registerDescription")}</span>
                                <Icon
                                    name="arrow"
                                    variant="outline"
                                    className="matching-entry__arrow"
                                />
                            </Button>
                            <Button
                                variant="plain"
                                className="matching-entry__choice"
                                onClick={() => {
                                    setMyPage(0);
                                    setSelectedRegistrationId(undefined);
                                    setAnimatePanelLayout(false);
                                    setView("mine");
                                }}
                            >
                                <Icon name="journey-list" />
                                <strong>{t("matchingEntry.mineTitle")}</strong>
                                <span>{t("matchingEntry.mineDescription")}</span>
                                <Icon
                                    name="arrow"
                                    variant="outline"
                                    className="matching-entry__arrow"
                                />
                            </Button>
                        </div>
                    </section>
                ) : view === "find" ? (
                    <section
                        className={`matching-content${editingId !== undefined ? " matching-content--editing" : ""}`}
                        aria-label={title}
                    >
                        {editingId !== undefined && (
                            <div className="matching-edit-context">
                                <span>
                                    <Icon name="edit" />
                                    {t("matchingFlow.editMode")}
                                </span>
                                <p>
                                    {matching.searches.find((item) => item.id === editingId)
                                        ?.title || t("matchingFlow.untitled")}
                                </p>
                            </div>
                        )}
                        <ol className="matching-progress" aria-label={t("matchingFlow.progress")}>
                            {(
                                [
                                    "destination",
                                    "period",
                                    "privacy",
                                    "preferences",
                                    "style",
                                    "register",
                                ] as const
                            ).map((name, index) => (
                                <li
                                    key={name}
                                    className={
                                        index === group
                                            ? "is-current"
                                            : index < group
                                              ? "is-complete"
                                              : "is-pending"
                                    }
                                    aria-current={index === group ? "step" : undefined}
                                >
                                    <span>
                                        <Icon name={progressIcons[index]!} />
                                    </span>
                                    <strong>
                                        {t(
                                            name === "register" && editingId !== undefined
                                                ? "matchingFlow.editReview"
                                                : `matchingFlow.${name}`,
                                        )}
                                    </strong>
                                    <Icon
                                        name="check"
                                        variant="outline"
                                        className="matching-progress__done"
                                    />
                                    {index < group && (
                                        <span className="visually-hidden">
                                            {t("matchingFlow.completed")}
                                        </span>
                                    )}
                                </li>
                            ))}
                        </ol>
                        <form
                            className={`matching-question${step === 1 ? " matching-question--dates" : step === 9 ? " matching-question--confirm" : ""}`}
                            noValidate
                            onSubmit={(event) => {
                                event.preventDefault();
                                if (matching.busy || !canContinue) return;
                                if (editingId === undefined && matching.limitReached) {
                                    setStatusNotice("activeLimit");
                                    return;
                                }
                                if (
                                    editingId === undefined &&
                                    (matching.catalogLoading || matching.catalogFailed)
                                )
                                    return;
                                if (step < 9) {
                                    goTo(step + 1);
                                    return;
                                }
                                if (!valid) {
                                    goTo(
                                        !selectedDestination
                                            ? 0
                                            : !datesValid
                                              ? 1
                                              : !agesValid
                                                ? 4
                                                : !stylesValid
                                                  ? 5
                                                  : 9,
                                    );
                                    return;
                                }
                                void matching.save(conditions, editingId).then((ok) => {
                                    if (ok) {
                                        setSavedEdit(editingId !== undefined);
                                        setSaved(true);
                                        setMyPage(0);
                                        setView("mine");
                                        setEditingId(undefined);
                                    } else if (
                                        matching.getFailureReason() ===
                                            "matchingFlow.activeLimit" ||
                                        matching.getFailureReason() ===
                                            "matchingFlow.travelStyleRequired"
                                    ) {
                                        setStatusNotice(
                                            matching.getFailureReason() ===
                                                "matchingFlow.activeLimit"
                                                ? "activeLimit"
                                                : "travelStyleRequired",
                                        );
                                    }
                                });
                            }}
                        >
                            <div className="matching-question__body" key={step}>
                                <header className="matching-heading">
                                    <h2>
                                        <Icon name={questionIcons[step]!} />
                                        {t(
                                            step === 9 && editingId !== undefined
                                                ? "matchingFlow.editReview"
                                                : `matchingFlow.questions.${questionKeys[step]!}`,
                                        )}
                                    </h2>
                                    {styleQuestion && step >= 5 && step <= 8 && (
                                        <p>
                                            {t(
                                                editingId === undefined
                                                    ? "matchingFlow.styleSelectionHint"
                                                    : "matchingFlow.styleEditHint",
                                            )}
                                        </p>
                                    )}
                                </header>
                                <div className="matching-question__content">
                                    {step === 0 && (
                                        <div className="matching-input-group matching-destination-input">
                                            {userId === undefined ? (
                                                <div className="matching-login">
                                                    <p>{t("matchingFlow.loginHint")}</p>
                                                    <Button variant="brand" onClick={login}>
                                                        {t("matchingFlow.login")}
                                                    </Button>
                                                </div>
                                            ) : matching.catalogLoading ? (
                                                <Spinner label={t("matchingFlow.loading")} />
                                            ) : (
                                                <Autocomplete
                                                    label={t("matchingFlow.destinationSearch")}
                                                    value={destinationId}
                                                    query={destinationQuery}
                                                    onQueryChange={(query) => {
                                                        setDestinationQuery(query);
                                                        setDestinationId("");
                                                    }}
                                                    onSelect={(option) => {
                                                        setDestinationId(option.value);
                                                        setDestinationQuery(option.label);
                                                    }}
                                                    placeholder={t(
                                                        "matchingFlow.destinationPlaceholder",
                                                    )}
                                                    emptyLabel={t("matchingFlow.noDestinations")}
                                                    disabled={matching.catalogFailed}
                                                    options={matching.destinations.map(
                                                        (destination) => ({
                                                            value: String(destination.id),
                                                            label: destination.name,
                                                        }),
                                                    )}
                                                />
                                            )}
                                            {matching.catalogFailed && (
                                                <div className="matching-error" role="alert">
                                                    <p>{t("matchingFlow.catalogError")}</p>
                                                    <Button
                                                        variant="plain"
                                                        className="matching-text-action"
                                                        onClick={matching.reload}
                                                    >
                                                        {t("matchingFlow.retry")}
                                                    </Button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                    {step === 1 && (
                                        <DateRangeCalendar
                                            label={t("matchingFlow.period")}
                                            startDate={startDate}
                                            endDate={endDate}
                                            onChange={(start, end) => {
                                                setStartDate(start);
                                                setEndDate(end);
                                            }}
                                        />
                                    )}
                                    {step === 3 && (
                                        <div className="matching-gender">
                                            <IconChoiceGroup
                                                showLabels
                                                label={t("matchingFlow.gender")}
                                                value={gender}
                                                onChange={setGender}
                                                options={[
                                                    {
                                                        value: "",
                                                        label: t("matchingFlow.any"),
                                                        icon: "gender-unset",
                                                    },
                                                    {
                                                        value: "MALE",
                                                        label: t("matchingFlow.male"),
                                                        icon: "gender-male",
                                                    },
                                                    {
                                                        value: "FEMALE",
                                                        label: t("matchingFlow.female"),
                                                        icon: "gender-female",
                                                    },
                                                ]}
                                            />
                                        </div>
                                    )}
                                    {step === 4 && (
                                        <div className="matching-age-range">
                                            <Checkbox
                                                label={t("matchingFlow.anyAge")}
                                                checked={anyAge}
                                                onChange={(event) =>
                                                    setAnyAge(event.target.checked)
                                                }
                                            />
                                            <RangeSelector
                                                label={t("matchingFlow.age")}
                                                lowerLabel={t("matchingFlow.minAge")}
                                                upperLabel={t("matchingFlow.maxAge")}
                                                lowerValueLabel={t("matchingFlow.ageValue", {
                                                    count: Number(minAge || 20),
                                                })}
                                                upperValueLabel={t("matchingFlow.ageValue", {
                                                    count: Number(maxAge || 40),
                                                })}
                                                value={[Number(minAge || 20), Number(maxAge || 40)]}
                                                min={19}
                                                max={100}
                                                disabled={anyAge}
                                                ticks={[19, 30, 40, 50, 60, 70, 80, 90, 100]}
                                                onChange={([lower, upper]) => {
                                                    setMinAge(String(lower));
                                                    setMaxAge(String(upper));
                                                }}
                                            />
                                        </div>
                                    )}
                                    {step === 2 && (
                                        <Card className="matching-privacy">
                                            <p>{t("matchingFlow.privacyHint")}</p>
                                            <div className="matching-privacy__options">
                                                <div className="matching-privacy__item">
                                                    <Icon
                                                        name={
                                                            profile?.gender === "MALE"
                                                                ? "gender-male"
                                                                : profile?.gender === "FEMALE"
                                                                  ? "gender-female"
                                                                  : "gender-unset"
                                                        }
                                                    />
                                                    <div>
                                                        <span>{t("settings.gender")}</span>
                                                        <strong>
                                                            {t(
                                                                profile?.gender === "MALE"
                                                                    ? "matchingFlow.male"
                                                                    : profile?.gender === "FEMALE"
                                                                      ? "matchingFlow.female"
                                                                      : "matchingFlow.profileMissing",
                                                            )}
                                                        </strong>
                                                    </div>
                                                    <Checkbox
                                                        label={t("matchingFlow.public")}
                                                        checked={showGender}
                                                        onChange={(event) =>
                                                            setShowGender(event.target.checked)
                                                        }
                                                    />
                                                </div>
                                                <div className="matching-privacy__item">
                                                    <Icon name="profile" />
                                                    <div>
                                                        <span>{t("matchingFlow.myAge")}</span>
                                                        <strong>
                                                            {profileAge === null
                                                                ? t("matchingFlow.profileMissing")
                                                                : t("matchingFlow.ageValue", {
                                                                      count: profileAge,
                                                                  })}
                                                        </strong>
                                                    </div>
                                                    <Checkbox
                                                        label={t("matchingFlow.public")}
                                                        checked={showAge}
                                                        onChange={(event) =>
                                                            setShowAge(event.target.checked)
                                                        }
                                                    />
                                                </div>
                                            </div>
                                        </Card>
                                    )}
                                    {styleQuestion && step >= 5 && step <= 8 && (
                                        <TravelStyleChoices
                                            question={styleQuestion}
                                            value={styleScore(styleQuestion)}
                                            onChange={(value) =>
                                                setStyles((current) => ({
                                                    ...current,
                                                    [styleQuestion]: String(value),
                                                }))
                                            }
                                        />
                                    )}
                                    {step === 9 && (
                                        <div className="matching-confirm">
                                            <Card className="matching-confirm__card">
                                                <TextField
                                                    label={t("matchingFlow.registrationTitle")}
                                                    maxLength={80}
                                                    value={displayedTitle}
                                                    onChange={(event) => {
                                                        setRegistrationTitle(event.target.value);
                                                        setTitleEdited(true);
                                                    }}
                                                    placeholder={t("matchingFlow.titlePlaceholder")}
                                                />
                                                <TextArea
                                                    label={t("matchingFlow.introduction")}
                                                    placeholder={t(
                                                        "matchingFlow.introductionPlaceholder",
                                                    )}
                                                    rows={3}
                                                    value={introduction}
                                                    onChange={(event) =>
                                                        setIntroduction(
                                                            Array.from(event.target.value)
                                                                .slice(0, 200)
                                                                .join(""),
                                                        )
                                                    }
                                                    hint={t("matchingFlow.introductionCount", {
                                                        count: Array.from(introduction).length,
                                                        max: 200,
                                                    })}
                                                />
                                                <details className="matching-confirm__review">
                                                    <summary>
                                                        {t("matchingFlow.reviewConditions")}
                                                        <Icon name="chevron" variant="outline" />
                                                    </summary>
                                                    <dl className="matching-conditions-summary">
                                                        <div>
                                                            <dt>
                                                                <Icon name="journey-place" />
                                                                {t("matchingFlow.destination")}
                                                            </dt>
                                                            <dd>{selectedDestination?.name}</dd>
                                                        </div>
                                                        <div>
                                                            <dt>
                                                                <Icon name="journey-dates" />
                                                                {t("matchingFlow.period")}
                                                            </dt>
                                                            <dd>
                                                                {t("matchingFlow.dateRange", {
                                                                    start: startDate
                                                                        ? date(startDate)
                                                                        : "",
                                                                    end: endDate
                                                                        ? date(endDate)
                                                                        : "",
                                                                })}
                                                            </dd>
                                                        </div>
                                                        <div>
                                                            <dt>
                                                                <Icon name="journey-people" />
                                                                {t("settings.gender")}
                                                            </dt>
                                                            <dd>
                                                                {t(
                                                                    gender === "MALE"
                                                                        ? "matchingFlow.male"
                                                                        : gender === "FEMALE"
                                                                          ? "matchingFlow.female"
                                                                          : "matchingFlow.any",
                                                                )}
                                                            </dd>
                                                        </div>
                                                        <div>
                                                            <dt>
                                                                <Icon name="journey-age" />
                                                                {t("matchingFlow.ageSummary")}
                                                            </dt>
                                                            <dd>
                                                                {anyAge
                                                                    ? t("matchingFlow.anyAge")
                                                                    : `${t("matchingFlow.ageValue", { count: Number(minAge) })} ~ ${t("matchingFlow.ageValue", { count: Number(maxAge) })}`}
                                                            </dd>
                                                        </div>
                                                        {styleQuestions.map((name) => {
                                                            const choice = styleScore(name);
                                                            return (
                                                                <div key={name}>
                                                                    <dt>
                                                                        <Icon
                                                                            name={
                                                                                name === "planning"
                                                                                    ? "journey-plan"
                                                                                    : name ===
                                                                                        "pace"
                                                                                      ? "journey-pace"
                                                                                      : name ===
                                                                                          "together"
                                                                                        ? "journey-together"
                                                                                        : "journey-budget"
                                                                            }
                                                                        />
                                                                        {t(
                                                                            `matchingFlow.styleLabels.${name}`,
                                                                        )}
                                                                    </dt>
                                                                    <dd>
                                                                        {styleDescription(
                                                                            name,
                                                                            choice,
                                                                        )}
                                                                    </dd>
                                                                </div>
                                                            );
                                                        })}
                                                        <div>
                                                            <dt>
                                                                <Icon name="profile" />
                                                                {t("matchingFlow.privacy")}
                                                            </dt>
                                                            <dd>
                                                                {t("matchingFlow.privacySummary", {
                                                                    gender: t(
                                                                        showGender
                                                                            ? "matchingFlow.public"
                                                                            : "matchingFlow.private",
                                                                    ),
                                                                    age: t(
                                                                        showAge
                                                                            ? "matchingFlow.public"
                                                                            : "matchingFlow.private",
                                                                    ),
                                                                })}
                                                            </dd>
                                                        </div>
                                                    </dl>
                                                </details>
                                            </Card>
                                            <Button
                                                type="submit"
                                                variant="brand"
                                                className="matching-confirm__submit"
                                                disabled={
                                                    !valid ||
                                                    userId === undefined ||
                                                    (editingId === undefined &&
                                                        (matching.catalogLoading ||
                                                            matching.catalogFailed))
                                                }
                                                loading={matching.busy}
                                            >
                                                <Icon name="check" variant="outline" />
                                                {t(
                                                    editingId !== undefined
                                                        ? "matchingFlow.saveChanges"
                                                        : "matchingFlow.save",
                                                )}
                                            </Button>
                                        </div>
                                    )}
                                    {matching.failed &&
                                        matching.errorKey !== "matchingFlow.activeLimit" &&
                                        matching.errorKey !==
                                            "matchingFlow.travelStyleRequired" && (
                                            <p className="matching-error" role="alert">
                                                {t(matching.errorKey)}
                                            </p>
                                        )}
                                </div>
                            </div>
                            <Tooltip
                                className="matching-step-nav matching-step-nav--previous"
                                content={t("matchingFlow.previous")}
                            >
                                <Button
                                    variant="plain"
                                    className="matching-step-arrow"
                                    aria-label={t("matchingFlow.previous")}
                                    disabled={step === 0 || matching.busy}
                                    onClick={() => goTo(step - 1)}
                                >
                                    <Icon
                                        name="arrow"
                                        variant="brand"
                                        className="matching-back-arrow"
                                    />
                                </Button>
                            </Tooltip>
                            {step < 9 && (
                                <Tooltip
                                    className="matching-step-nav matching-step-nav--next"
                                    content={t(
                                        step === 9 ? "matchingFlow.save" : "matchingFlow.next",
                                    )}
                                >
                                    <Button
                                        type="submit"
                                        variant="plain"
                                        className="matching-step-arrow"
                                        aria-label={t(
                                            step === 9 ? "matchingFlow.save" : "matchingFlow.next",
                                        )}
                                        aria-busy={matching.busy || undefined}
                                        disabled={
                                            !canContinue ||
                                            userId === undefined ||
                                            matching.busy ||
                                            (editingId === undefined &&
                                                (matching.catalogLoading || matching.catalogFailed))
                                        }
                                    >
                                        <Icon
                                            name={step === 9 ? "check" : "arrow"}
                                            variant="brand"
                                        />
                                    </Button>
                                </Tooltip>
                            )}
                        </form>
                    </section>
                ) : (
                    <section
                        className="matching-content matching-content--mine"
                        aria-label={t("matchingFlow.mine")}
                    >
                        {saved && (
                            <Toast
                                message={t(
                                    savedEdit ? "matchingFlow.updated" : "matchingFlow.saved",
                                )}
                                tone="success"
                                onDismiss={() => setSaved(false)}
                            />
                        )}
                        {userId === undefined && (
                            <Button onClick={login}>{t("matchingFlow.login")}</Button>
                        )}
                        {matching.catalogLoading && <Spinner label={t("matchingFlow.loading")} />}
                        {(matching.catalogFailed ||
                            (matching.failed &&
                                matching.errorKey !== "matchingFlow.activeLimit" &&
                                matching.errorKey !== "matchingFlow.travelStyleRequired")) && (
                            <p role="alert" className="matching-error">
                                {t(matching.errorKey)}
                            </p>
                        )}
                        {!matching.catalogLoading &&
                            !matching.catalogFailed &&
                            userId !== undefined &&
                            matching.searches.length === 0 && (
                                <div className="matching-empty">
                                    <p>{t("matchingFlow.noRegistrations")}</p>
                                    <Button
                                        onClick={() => {
                                            setView("find");
                                            goTo(0);
                                        }}
                                    >
                                        {t("matchingFlow.find")}
                                    </Button>
                                </div>
                            )}
                        <div
                            className={`matching-recruitments${selectedRegistration ? " matching-recruitments--with-panel" : ""}`}
                        >
                            <div className="matching-recruitments__list">
                                <header className="matching-recruitments__heading">
                                    <h2>
                                        <Icon name="journey-list" />
                                        {t("matchingFlow.myPosts")}
                                    </h2>
                                    {!matching.catalogLoading &&
                                        !matching.catalogFailed &&
                                        userId !== undefined && (
                                            <p>
                                                {t("matchingFlow.myPostsCount", {
                                                    count: filteredSearches.length,
                                                })}
                                            </p>
                                        )}
                                    <div className="matching-recruitments__filters">
                                        <div className="matching-filter-search">
                                            <Icon name="search" />
                                            <TextField
                                                label={t("matchingFlow.searchMyPosts")}
                                                type="search"
                                                placeholder={t("matchingFlow.searchMyPosts")}
                                                value={mySearchQuery}
                                                onChange={(event) => {
                                                    setMySearchQuery(event.target.value);
                                                    setMyPage(0);
                                                    setSelectedRegistrationId(undefined);
                                                }}
                                            />
                                        </div>
                                        {[
                                            {
                                                label: t("matchingFlow.destination"),
                                                value: myDestinationFilter,
                                                icon: "pin" as const,
                                                set: setMyDestinationFilter,
                                                options: [
                                                    {
                                                        value: "",
                                                        label: t("matchingFlow.placeFilter"),
                                                    },
                                                    ...matching.destinations.map((item) => ({
                                                        value: String(item.id),
                                                        label: item.name,
                                                    })),
                                                ],
                                            },
                                            {
                                                label: t("matchingFlow.statusFilter"),
                                                value: myStatusFilter,
                                                icon: "journey-register" as const,
                                                set: setMyStatusFilter,
                                                options: [
                                                    {
                                                        value: "all",
                                                        label: t("matchingFlow.statusFilter"),
                                                    },
                                                    {
                                                        value: "ACTIVE",
                                                        label: t("matchingFlow.active"),
                                                    },
                                                    {
                                                        value: "CLOSED",
                                                        label: t("matchingFlow.closed"),
                                                    },
                                                ],
                                            },
                                        ].map((filter) => (
                                            <DropdownMenu
                                                key={filter.label}
                                                label={filter.label}
                                                value={filter.value}
                                                options={filter.options}
                                                icon={filter.icon}
                                                onChange={(value) => {
                                                    filter.set(value);
                                                    setMyPage(0);
                                                    setSelectedRegistrationId(undefined);
                                                }}
                                            />
                                        ))}
                                        <MatchingFilterPopover
                                            className="matching-filter-date-trigger"
                                            icon="calendar"
                                            label={
                                                myStartFilter
                                                    ? `${filterDate(myStartFilter)} ~ ${myEndFilter ? filterDate(myEndFilter) : "…"}`
                                                    : t("matchingFlow.period")
                                            }
                                        >
                                            <DateRangeCalendar
                                                label={t("matchingFlow.period")}
                                                startDate={myStartFilter}
                                                endDate={myEndFilter}
                                                onChange={(start, end) => {
                                                    setMyStartFilter(start);
                                                    setMyEndFilter(end);
                                                    setMyPage(0);
                                                    setSelectedRegistrationId(undefined);
                                                }}
                                            />
                                        </MatchingFilterPopover>
                                        <MatchingFilterPopover
                                            icon="journey-age"
                                            label={
                                                myAgeFilter
                                                    ? `${myAgeRange[0]} ~ ${myAgeRange[1]}`
                                                    : t("matchingFlow.ageFilter")
                                            }
                                        >
                                            <Checkbox
                                                label={t("matchingFlow.anyAge")}
                                                checked={!myAgeFilter}
                                                onChange={(event) => {
                                                    setMyAgeFilter(!event.target.checked);
                                                    setMyPage(0);
                                                    setSelectedRegistrationId(undefined);
                                                }}
                                            />
                                            <RangeSelector
                                                label={t("matchingFlow.ageFilter")}
                                                lowerLabel={t("matchingFlow.minAge")}
                                                upperLabel={t("matchingFlow.maxAge")}
                                                lowerValueLabel={String(myAgeRange[0])}
                                                upperValueLabel={String(myAgeRange[1])}
                                                min={19}
                                                max={100}
                                                ticks={[19, 30, 40, 50, 60, 70, 80, 90, 100]}
                                                value={myAgeRange}
                                                disabled={!myAgeFilter}
                                                onChange={(range) => {
                                                    setMyAgeRange(range);
                                                    setMyAgeFilter(true);
                                                    setMyPage(0);
                                                    setSelectedRegistrationId(undefined);
                                                }}
                                            />
                                        </MatchingFilterPopover>
                                        <IconChoiceGroup
                                            label={t("matchingFlow.gender")}
                                            value={myGenderFilter}
                                            options={[
                                                {
                                                    value: "",
                                                    label: t("matchingFlow.any"),
                                                    icon: "gender-unset",
                                                },
                                                {
                                                    value: "MALE",
                                                    label: t("matchingFlow.male"),
                                                    icon: "gender-male",
                                                },
                                                {
                                                    value: "FEMALE",
                                                    label: t("matchingFlow.female"),
                                                    icon: "gender-female",
                                                },
                                            ]}
                                            onChange={(value) => {
                                                setMyGenderFilter(value);
                                                setMyPage(0);
                                                setSelectedRegistrationId(undefined);
                                            }}
                                        />
                                        <Button
                                            variant="plain"
                                            className="matching-text-action"
                                            onClick={() => {
                                                setMySearchQuery("");
                                                setMyDestinationFilter("");
                                                setMyGenderFilter("");
                                                setMyAgeFilter(false);
                                                setMyStartFilter("");
                                                setMyEndFilter("");
                                                setMyStatusFilter("all");
                                                setMyPage(0);
                                                setSelectedRegistrationId(undefined);
                                            }}
                                        >
                                            {t("matchingFlow.resetFilters")}
                                        </Button>
                                    </div>
                                </header>
                                {!matching.catalogLoading &&
                                    matching.searches.length > 0 &&
                                    filteredSearches.length === 0 && (
                                        <div className="matching-filter-empty" role="status">
                                            <div
                                                className="matching-filter-empty__illustration"
                                                aria-hidden="true"
                                            >
                                                <Icon name="search" />
                                            </div>
                                            <h3>{t("matchingFlow.noFilteredPosts")}</h3>
                                            <p>{t("matchingFlow.adjustFilters")}</p>
                                        </div>
                                    )}
                                <div
                                    className="matching-registration-grid"
                                    hidden={visibleSearches.length === 0}
                                >
                                    {visibleSearches.map((registration) => (
                                        <Card
                                            className={`matching-registration${selectedRegistrationId === registration.id ? " is-selected" : ""}`}
                                            key={registration.id}
                                        >
                                            <div>
                                                <Badge
                                                    tone={
                                                        registration.status === "ACTIVE"
                                                            ? "brand"
                                                            : "neutral"
                                                    }
                                                    label={t(
                                                        registration.status === "ACTIVE"
                                                            ? "matchingFlow.active"
                                                            : "matchingFlow.closed",
                                                    )}
                                                />
                                                <h3>
                                                    <Button
                                                        variant="plain"
                                                        className="matching-registration__open"
                                                        aria-expanded={
                                                            selectedRegistrationId ===
                                                            registration.id
                                                        }
                                                        aria-controls={
                                                            selectedRegistrationId ===
                                                            registration.id
                                                                ? "recruitment-details-panel"
                                                                : undefined
                                                        }
                                                        onClick={() => {
                                                            setAnimatePanelLayout(true);
                                                            setSelectedRegistrationId((current) =>
                                                                current === registration.id
                                                                    ? undefined
                                                                    : registration.id,
                                                            );
                                                        }}
                                                    >
                                                        {registration.title ||
                                                            t("matchingFlow.untitled")}
                                                    </Button>
                                                </h3>
                                                <p>
                                                    <Icon name="journey-place" />
                                                    {matching.destinations.find(
                                                        (value) =>
                                                            value.id === registration.destinationId,
                                                    )?.name || registration.legacyRegion}
                                                </p>
                                                <p>
                                                    <Icon name="journey-dates" />
                                                    {t("matchingFlow.dateRange", {
                                                        start: date(registration.startDate),
                                                        end: date(registration.endDate),
                                                    })}
                                                </p>
                                            </div>
                                            {registration.status === "ACTIVE" && (
                                                <div className="matching-registration__actions">
                                                    <Button
                                                        variant="plain"
                                                        className="matching-text-action"
                                                        disabled={
                                                            matching.busy ||
                                                            !registration.destinationId
                                                        }
                                                        onClick={() =>
                                                            editRegistration(registration.id)
                                                        }
                                                    >
                                                        <Icon name="edit" />
                                                        {t("matchingFlow.edit")}
                                                    </Button>
                                                    <Button
                                                        variant="plain"
                                                        className="matching-text-action"
                                                        disabled={matching.busy}
                                                        onClick={() => requestEnd(registration.id)}
                                                    >
                                                        {t("matchingFlow.close")}
                                                    </Button>
                                                </div>
                                            )}
                                            {registration.status === "CLOSED" && (
                                                <div className="matching-registration__actions">
                                                    <Tooltip
                                                        content={t(
                                                            !matching.destinations.some(
                                                                (item) =>
                                                                    item.id ===
                                                                    registration.destinationId,
                                                            )
                                                                ? "matchingFlow.resumeDestinationUnavailable"
                                                                : "matchingFlow.resume",
                                                        )}
                                                    >
                                                        <Button
                                                            variant="plain"
                                                            className="matching-text-action"
                                                            disabled={
                                                                matching.busy ||
                                                                matching.catalogLoading ||
                                                                matching.catalogFailed ||
                                                                !matching.destinations.some(
                                                                    (item) =>
                                                                        item.id ===
                                                                        registration.destinationId,
                                                                )
                                                            }
                                                            onClick={() =>
                                                                requestEnd(
                                                                    registration.id,
                                                                    "resume",
                                                                )
                                                            }
                                                        >
                                                            {t("matchingFlow.resume")}
                                                        </Button>
                                                    </Tooltip>
                                                </div>
                                            )}
                                        </Card>
                                    ))}
                                </div>
                                {pageCount > 1 && (
                                    <nav
                                        className="matching-list-pagination"
                                        aria-label={t("matchingFlow.pagination")}
                                    >
                                        <Button
                                            variant="plain"
                                            className="matching-text-action"
                                            disabled={currentPage === 0 || matching.busy}
                                            onClick={() => {
                                                setMyPage(currentPage - 1);
                                                setSelectedRegistrationId(undefined);
                                            }}
                                        >
                                            {t("matchingFlow.previous")}
                                        </Button>
                                        <span aria-live="polite">
                                            {t("matchingFlow.pagePosition", {
                                                page: currentPage + 1,
                                                total: pageCount,
                                            })}
                                        </span>
                                        <Button
                                            variant="plain"
                                            className="matching-text-action"
                                            disabled={currentPage >= pageCount - 1 || matching.busy}
                                            onClick={() => {
                                                setMyPage(currentPage + 1);
                                                setSelectedRegistrationId(undefined);
                                            }}
                                        >
                                            {t("matchingFlow.next")}
                                        </Button>
                                    </nav>
                                )}
                            </div>
                            {selectedRegistration && (
                                <RecruitmentDetailsPanel
                                    registration={selectedRegistration}
                                    destination={
                                        matching.destinations.find(
                                            (destination) =>
                                                destination.id ===
                                                selectedRegistration.destinationId,
                                        )?.name ||
                                        selectedRegistration.legacyRegion ||
                                        ""
                                    }
                                    busy={matching.busy}
                                    failed={
                                        matching.failed &&
                                        matching.errorKey !== "matchingFlow.activeLimit" &&
                                        matching.errorKey !== "matchingFlow.travelStyleRequired"
                                    }
                                    onClose={() => setSelectedRegistrationId(undefined)}
                                    onEdit={() => editRegistration(selectedRegistration.id)}
                                    onFinish={() => requestEnd(selectedRegistration.id)}
                                    onResume={() => requestEnd(selectedRegistration.id, "resume")}
                                    resumeDisabled={
                                        matching.catalogLoading ||
                                        matching.catalogFailed ||
                                        !matching.destinations.some(
                                            (item) =>
                                                item.id === selectedRegistration.destinationId,
                                        )
                                    }
                                    resumeHint={t(
                                        !matching.destinations.some(
                                            (item) =>
                                                item.id === selectedRegistration.destinationId,
                                        )
                                            ? "matchingFlow.resumeDestinationUnavailable"
                                            : "matchingFlow.resume",
                                    )}
                                    errorMessage={t(matching.errorKey)}
                                />
                            )}
                        </div>
                    </section>
                )}
            </div>
            {statusNotice && (
                <Toast
                    message={t(`matchingFlow.${statusNotice}`)}
                    tone={
                        statusNotice === "activeLimit" || statusNotice === "travelStyleRequired"
                            ? "error"
                            : "success"
                    }
                    onDismiss={() => setStatusNotice(undefined)}
                />
            )}
            {endingRegistration && (
                <EndRecruitmentDialog
                    title={endingRegistration.title || t("matchingFlow.untitled")}
                    busy={matching.busy}
                    action={statusAction}
                    errorMessage={t(matching.errorKey)}
                    failed={endFailed}
                    onCancel={() => {
                        if (!matching.busy) setEndingId(undefined);
                    }}
                    onConfirm={() => void confirmEnd()}
                />
            )}
        </main>
    );
}
