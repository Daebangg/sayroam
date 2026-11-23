import { recruitmentResultsUrl, pagePaths } from "@/routing/paths";
import { useMatchedRecruitments } from "@/hooks/api/matching/useRecruitmentResults";
import { RecruitmentResultsPage } from "./RecruitmentResultsPage";
import { RecruitmentTextAction } from "@/components/features/matching/RecruitmentTextAction";
import { useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import { Navigate, useLocation, useNavigate, useOutletContext } from "react-router";
import {
    Button,
    PageState,
    Card,
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
import { PageLeaveDialog } from "@/components/layout/PageLeaveDialog";
import {
    RecruitmentActionDialog,
    type RecruitmentAction,
} from "@/components/features/matching/RecruitmentActionDialog";
import { RecruitmentStatusBadge } from "@/components/features/matching/RecruitmentStatusBadge";
import { RecruitmentDetailsPanel } from "@/components/features/matching/RecruitmentDetailsPanel";
import { TravelStyleChoices } from "@/components/features/matching/TravelStyleChoices";
import { RoamDiscoveryLandscape } from "@/components/features/matching/RoamDiscoveryLandscape";
import { MatchingFilterPopover } from "@/components/features/matching/MatchingFilterPopover";
import type { MatchingRegistration } from "@/hooks/matching.types";
import type { PageContext } from "@/routing/context";

type MatchingDraft = {
    destinationQuery: string;
    startDate: string;
    endDate: string;
    gender: string;
    minAge: string;
    maxAge: string;
    anyAge: boolean;
};

const emptyDraft: MatchingDraft = {
    destinationQuery: "",
    startDate: "",
    endDate: "",
    gender: "",
    minAge: "20",
    maxAge: "40",
    anyAge: true,
};

type MatchingData = Omit<
    ReturnType<typeof useCompanionMatching>,
    "preview" | "clearCandidates" | "candidates" | "activeCount"
>;
export type RecruitmentPreview = {
    data: MatchingData;
    matchedIds: ReadonlySet<number>;
    openResults: (id: number) => void;
    startDiscovery: (id: number, origin: "create" | "edit" | "resume") => void;
};

export function MatchingPage({ preview }: { preview?: RecruitmentPreview }) {
    const { member } = useOutletContext<PageContext>();
    return <MatchingSession key={member?.userId ?? "guest"} preview={preview} />;
}

function MatchingSession({ preview }: { preview: RecruitmentPreview | undefined }) {
    const { t, i18n } = useTranslation();
    const { member, onOpenAuth } = useOutletContext<PageContext>();
    const [draft, setDraft] = useState<MatchingDraft>(emptyDraft);
    const location = useLocation();
    const live = useCompanionMatching(preview ? undefined : member?.userId);
    const matching = preview?.data ?? live;
    const navigate = useNavigate();
    const resultsParam = preview ? null : new URLSearchParams(location.search).get("searchId");
    const available = useMatchedRecruitments(
        live.searches,
        !preview && !!member && location.state?.matchingView === "mine" && resultsParam === null,
    );
    const openResults = (id: number, from: "create" | "edit" | "resume" | "list") =>
        void navigate(recruitmentResultsUrl(id), {
            replace: from === "create" || from === "edit",
            state: { resultsFrom: from },
        });
    if (resultsParam !== null && member) {
        if (live.catalogLoading) return <Spinner label={t("matchingFlow.loading")} />;
        const post = live.searches.find((item) => String(item.id) === resultsParam);
        if (live.catalogFailed) {
            return (
                <PageState
                    kind="error"
                    title={t("matchingFlow.resultsUnavailable")}
                    description={t("matchingFlow.resultsRetryHint")}
                    actions={
                        <>
                            <Button onClick={live.reload}>{t("inbox.retry")}</Button>
                            <Button
                                variant="soft"
                                onClick={() =>
                                    void navigate(pagePaths.matching, {
                                        replace: true,
                                        state: { matchingView: "mine" },
                                    })
                                }
                            >
                                {t("matchingEntry.mineTitle")}
                            </Button>
                        </>
                    }
                />
            );
        }
        if (
            !post ||
            post.status !== "ACTIVE" ||
            post.matchingStatus === "SKIPPED" ||
            !post.destinationId
        ) {
            return <Navigate to={pagePaths.matching} replace state={{ matchingView: "mine" }} />;
        }
        return (
            <RecruitmentResultsPage
                key={`${location.key}-${post.id}-${post.matchingRevision}`}
                post={post}
                from={location.state?.resultsFrom}
                destination={
                    live.destinations.find((d) => d.id === post.destinationId)?.name ??
                    post.legacyRegion ??
                    ""
                }
                reloadPost={live.reload}
                close={live.close}
                busy={live.busy}
                failed={live.failed}
            />
        );
    }
    const editId: unknown =
        location.state?.matchingView === "find" ? location.state.editId : undefined;
    const registration = matching.searches.find((post) => post.id === editId);
    if (editId !== undefined && matching.catalogLoading)
        return <Spinner label={t("matchingFlow.loading")} />;
    return (
        <MatchingWorkspace
            key={location.state?.matchingView === "mine" ? "mine" : (registration?.id ?? "new")}
            canManage={preview !== undefined || member?.userId !== undefined}
            matching={matching}
            matchedIds={preview?.matchedIds ?? available.matchedIds}
            openResults={preview?.openResults ?? ((id) => openResults(id, "list"))}
            startDiscovery={preview?.startDiscovery ?? openResults}
            resultsCheckFailed={!preview && available.failed}
            registration={registration}
            profile={member}
            initialDraft={draft}
            onDiscard={() => setDraft(emptyDraft)}
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
    canManage: boolean;
    matching: MatchingData;
    matchedIds: RecruitmentPreview["matchedIds"];
    openResults: RecruitmentPreview["openResults"];
    startDiscovery: RecruitmentPreview["startDiscovery"];
    resultsCheckFailed: boolean;
    registration: MatchingRegistration | undefined;
    profile: PageContext["member"];
    onDiscard: () => void;
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
    canManage,
    matching,
    matchedIds,
    openResults,
    startDiscovery,
    resultsCheckFailed,
    registration,
    profile,
    onLogin,
    initialDraft,
    onDiscard,
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
    const [step, setStep] = useState(0);
    const location = useLocation();
    const navigate = useNavigate();
    const section: unknown = location.state?.matchingView;
    const view = section === "mine" || section === "find" ? section : "entry";
    const savedDeparture = useRef(false);
    function setView(next: "mine" | "find", editId?: number): void {
        void navigate(
            { pathname: location.pathname, search: location.search, hash: location.hash },
            {
                replace: view !== "entry" && editId === undefined,
                state: { matchingView: next, editId },
            },
        );
    }
    const [destinationId, setDestinationId] = useState(String(registration?.destinationId ?? ""));
    const [destinationQuery, setDestinationQuery] = useState(initialDraft.destinationQuery);
    const [startDate, setStartDate] = useState(registration?.startDate ?? initialDraft.startDate);
    const [endDate, setEndDate] = useState(registration?.endDate ?? initialDraft.endDate);
    const [registrationTitle, setRegistrationTitle] = useState(registration?.title ?? "");
    const [introduction, setIntroduction] = useState(registration?.introduction ?? "");
    const [showAge, setShowAge] = useState(registration?.showAge ?? false);
    const [showGender, setShowGender] = useState(registration?.showGender ?? false);
    const [titleEdited, setTitleEdited] = useState(Boolean(registration?.title));
    const [gender, setGender] = useState(registration?.preferredGender ?? initialDraft.gender);
    const [minAge, setMinAge] = useState(
        registration ? String(registration.preferredMinAge ?? "") : initialDraft.minAge,
    );
    const [anyAge, setAnyAge] = useState(
        registration
            ? registration.preferredMinAge === null && registration.preferredMaxAge === null
            : initialDraft.anyAge,
    );
    const [maxAge, setMaxAge] = useState(
        registration ? String(registration.preferredMaxAge ?? "") : initialDraft.maxAge,
    );
    const lowerAge = Number(minAge || 19);
    const upperAge = Number(maxAge || 100);
    const ageLabel = (age: string) =>
        age ? t("matchingFlow.ageValue", { count: Number(age) }) : t("matchingFlow.any");
    const editingId = registration?.id;
    const [styles, setStyles] = useState<Record<string, string>>(() =>
        Object.fromEntries(
            styleQuestions
                .map((name) => [name, String(registration?.travelStyle[styleCodes[name]] ?? "")])
                .filter(([, value]) => value !== ""),
        ),
    );
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
    function listingStatus(post: MatchingRegistration) {
        return post.status === "ACTIVE" && matchedIds.has(post.id) ? "MATCHED" : post.status;
    }
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
            (myStatusFilter === "all" || listingStatus(registration) === myStatusFilter)
        );
    });
    const myPageSize = 9;
    const pageCount = Math.max(1, Math.ceil(filteredSearches.length / myPageSize));
    const currentPage = Math.min(myPage, pageCount - 1);
    const visibleSearches = filteredSearches.slice(
        currentPage * myPageSize,
        (currentPage + 1) * myPageSize,
    );
    const [pendingAction, setPendingAction] = useState<{ id: number; action: RecruitmentAction }>();
    const [statusNotice, setStatusNotice] = useState<
        "closedNotice" | "resumedNotice" | "activeLimit" | "travelStyleRequired"
    >();
    const [endFailed, setEndFailed] = useState(false);
    const pendingRegistration = matching.searches.find((item) => item.id === pendingAction?.id);
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

        setView("find", id);
    }
    function discardDraft(): void {
        onDiscard();
        setDestinationId("");
        setDestinationQuery("");
        setStartDate("");
        setEndDate("");
        setRegistrationTitle("");
        setIntroduction("");
        setShowAge(false);
        setShowGender(false);
        setTitleEdited(false);
        setGender("");
        setMinAge("20");
        setMaxAge("40");
        setAnyAge(true);
        setStyles({});
        goTo(0);
    }
    function requestAction(id: number, action: RecruitmentAction): void {
        if (matching.busy) return;
        if (action === "edit" && !matchedIds.has(id)) {
            editRegistration(id);
            return;
        }
        if (action === "resume" && matching.limitReached) {
            setStatusNotice("activeLimit");
            return;
        }
        if (action === "resume" && !matching.canCreate) return;
        setEndFailed(false);
        setPendingAction({ id, action });
    }
    async function confirmAction(): Promise<void> {
        if (!pendingAction || matching.busy) return;
        const { id, action } = pendingAction;
        if (action === "edit") {
            setPendingAction(undefined);
            editRegistration(id);
            return;
        }
        const ok = action === "resume" ? await matching.resume(id) : await matching.close(id);
        if (ok) {
            setPendingAction(undefined);
            if (action === "resume") startDiscovery(id, "resume");
            else setStatusNotice("closedNotice");
        } else if (
            matching.getFailureReason() === "matchingFlow.activeLimit" ||
            matching.getFailureReason() === "matchingFlow.travelStyleRequired"
        ) {
            setPendingAction(undefined);
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
        });
    }
    return (
        <main
            id="main-content"
            tabIndex={-1}
            className={`matching-workspace${view === "entry" ? " matching-workspace--entry" : ""}${view === "mine" ? " matching-workspace--mine" : ""}${view === "mine" && animatePanelLayout ? " matching-workspace--panel-transition" : ""}${view === "mine" && selectedRegistration ? " matching-workspace--details" : ""}`}
        >
            <RoamDiscoveryLandscape subtle />
            <div key={view} className="matching-view page-enter">
                {view !== "entry" && (
                    <div className="matching-section-nav">
                        <Button
                            variant="plain"
                            className="settings-back matching-back"
                            disabled={matching.busy}
                            onClick={() => void navigate(-1)}
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
                                    (canManage &&
                                        (matching.catalogLoading || matching.catalogFailed))
                                }
                                onClick={() => {
                                    if (matching.limitReached) {
                                        setStatusNotice("activeLimit");
                                        return;
                                    }
                                    if (canManage && !matching.canCreate) return;
                                    discardDraft();
                                    savedDeparture.current = false;
                                    setView("find");
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
                                onClick={() => setView("mine")}
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
                                        savedDeparture.current = true;
                                        setMyPage(0);
                                        startDiscovery(
                                            ok.id,
                                            editingId === undefined ? "create" : "edit",
                                        );
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
                                            {!canManage ? (
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
                                                onChange={(event) => {
                                                    setAnyAge(event.target.checked);
                                                    if (
                                                        !event.target.checked &&
                                                        !minAge &&
                                                        !maxAge
                                                    ) {
                                                        setMinAge(emptyDraft.minAge);
                                                        setMaxAge(emptyDraft.maxAge);
                                                    }
                                                }}
                                            />
                                            <RangeSelector
                                                label={t("matchingFlow.age")}
                                                lowerLabel={t(
                                                    minAge || anyAge
                                                        ? "matchingFlow.minAge"
                                                        : "matchingFlow.noMinAge",
                                                )}
                                                upperLabel={t(
                                                    maxAge || anyAge
                                                        ? "matchingFlow.maxAge"
                                                        : "matchingFlow.noMaxAge",
                                                )}
                                                lowerValueLabel={ageLabel(minAge)}
                                                upperValueLabel={ageLabel(maxAge)}
                                                value={[lowerAge, upperAge]}
                                                min={19}
                                                max={100}
                                                disabled={anyAge}
                                                ticks={[19, 30, 40, 50, 60, 70, 80, 90, 100]}
                                                onChange={([lower, upper]) => {
                                                    if (lower !== lowerAge)
                                                        setMinAge(String(lower));
                                                    if (upper !== upperAge)
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
                                                                    : `${ageLabel(minAge)} ~ ${ageLabel(maxAge)}`}
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
                                                className="step-action-button"
                                                disabled={
                                                    !valid ||
                                                    !canManage ||
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
                                            !canManage ||
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
                        {!canManage && <Button onClick={login}>{t("matchingFlow.login")}</Button>}
                        {matching.catalogLoading && <Spinner label={t("matchingFlow.loading")} />}
                        {(matching.catalogFailed ||
                            (matching.failed &&
                                matching.errorKey !== "matchingFlow.activeLimit" &&
                                matching.errorKey !== "matchingFlow.travelStyleRequired")) && (
                            <p role="alert" className="matching-error">
                                {t(matching.errorKey)}
                            </p>
                        )}
                        <div
                            className={`matching-recruitments${selectedRegistration ? " matching-recruitments--with-panel" : ""}`}
                        >
                            <div className="matching-recruitments__list">
                                {resultsCheckFailed && (
                                    <p role="status" className="matching-error">
                                        {t("matchingFlow.resultsRetryHint")}
                                    </p>
                                )}
                                <header className="matching-recruitments__heading">
                                    <h2>
                                        <Icon name="journey-list" />
                                        {t("matchingFlow.myPosts")}
                                    </h2>
                                    {!matching.catalogLoading &&
                                        !matching.catalogFailed &&
                                        canManage && (
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
                                                        value: "MATCHED",
                                                        label: t("roamMatePreview.matched"),
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
                                                setMyAgeRange([20, 40]);
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
                                    !matching.catalogFailed &&
                                    canManage &&
                                    filteredSearches.length === 0 && (
                                        <div className="matching-filter-empty" role="status">
                                            <div
                                                className="matching-filter-empty__illustration"
                                                aria-hidden="true"
                                            >
                                                <Icon
                                                    name={
                                                        matching.searches.length === 0
                                                            ? "journey-register"
                                                            : "search"
                                                    }
                                                />
                                            </div>
                                            <h3>
                                                {t(
                                                    matching.searches.length === 0
                                                        ? "matchingFlow.noRegistrations"
                                                        : "matchingFlow.noFilteredPosts",
                                                )}
                                            </h3>
                                            {matching.searches.length === 0 ? (
                                                <Button
                                                    variant="brand"
                                                    onClick={() => {
                                                        savedDeparture.current = false;
                                                        setView("find");
                                                        goTo(0);
                                                    }}
                                                >
                                                    {t("matchingEntry.registerTitle")}
                                                </Button>
                                            ) : (
                                                <p>{t("matchingFlow.adjustFilters")}</p>
                                            )}
                                        </div>
                                    )}
                                <div
                                    className="matching-registration-grid"
                                    hidden={visibleSearches.length === 0}
                                >
                                    {visibleSearches.map((registration) => (
                                        <Card
                                            className={`matching-registration${listingStatus(registration) === "MATCHED" ? " matching-registration--has-results" : ""}${selectedRegistrationId === registration.id ? " is-selected" : ""}`}
                                            key={registration.id}
                                        >
                                            <div>
                                                <RecruitmentStatusBadge
                                                    status={listingStatus(registration)}
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
                                                    {listingStatus(registration) === "MATCHED" && (
                                                        <Button
                                                            variant="brand"
                                                            className="matching-results-link"
                                                            onClick={() =>
                                                                openResults(registration.id)
                                                            }
                                                        >
                                                            <Icon name="matching" />
                                                            {t("roamMatePreview.viewResults")}
                                                            <Icon name="arrow" variant="outline" />
                                                        </Button>
                                                    )}
                                                    <RecruitmentTextAction
                                                        action="edit"
                                                        disabled={
                                                            matching.busy ||
                                                            !registration.destinationId
                                                        }
                                                        onClick={() =>
                                                            requestAction(registration.id, "edit")
                                                        }
                                                    />
                                                    <RecruitmentTextAction
                                                        action="close"
                                                        disabled={matching.busy}
                                                        onClick={() =>
                                                            requestAction(registration.id, "close")
                                                        }
                                                    />
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
                                                        <RecruitmentTextAction
                                                            action="resume"
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
                                                                requestAction(
                                                                    registration.id,
                                                                    "resume",
                                                                )
                                                            }
                                                        />
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
                            <RecruitmentDetailsPanel
                                details={
                                    selectedRegistration
                                        ? {
                                              registration: selectedRegistration,
                                              destination:
                                                  matching.destinations.find(
                                                      (destination) =>
                                                          destination.id ===
                                                          selectedRegistration.destinationId,
                                                  )?.name ||
                                                  selectedRegistration.legacyRegion ||
                                                  "",
                                              busy: matching.busy,
                                              failed:
                                                  matching.failed &&
                                                  matching.errorKey !==
                                                      "matchingFlow.activeLimit" &&
                                                  matching.errorKey !==
                                                      "matchingFlow.travelStyleRequired",
                                              onClose: () => setSelectedRegistrationId(undefined),
                                              ...(listingStatus(selectedRegistration) ===
                                                  "MATCHED" && {
                                                  onViewResults: () =>
                                                      openResults(selectedRegistration.id),
                                              }),
                                              onEdit: () =>
                                                  requestAction(selectedRegistration.id, "edit"),
                                              onFinish: () =>
                                                  requestAction(selectedRegistration.id, "close"),
                                              onResume: () =>
                                                  requestAction(selectedRegistration.id, "resume"),
                                              resumeDisabled:
                                                  matching.catalogLoading ||
                                                  matching.catalogFailed ||
                                                  !matching.destinations.some(
                                                      (item) =>
                                                          item.id ===
                                                          selectedRegistration.destinationId,
                                                  ),
                                              resumeHint: t(
                                                  !matching.destinations.some(
                                                      (item) =>
                                                          item.id ===
                                                          selectedRegistration.destinationId,
                                                  )
                                                      ? "matchingFlow.resumeDestinationUnavailable"
                                                      : "matchingFlow.resume",
                                              ),
                                              errorMessage: t(matching.errorKey),
                                          }
                                        : undefined
                                }
                            />
                        </div>
                    </section>
                )}
            </div>
            {view === "find" && (
                <PageLeaveDialog
                    shouldBlock={() => !savedDeparture.current}
                    title={t("matchingFlow.discardTitle")}
                    description={t("matchingFlow.discardHint")}
                    stayLabel={t("matchingFlow.keepEditing")}
                    leaveLabel={t("profileSetup.leave")}
                    leaveDisabled={matching.busy}
                    onLeave={discardDraft}
                />
            )}
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
            {view === "mine" && pendingAction && pendingRegistration && (
                <RecruitmentActionDialog
                    title={pendingRegistration.title || t("matchingFlow.untitled")}
                    busy={matching.busy}
                    action={pendingAction.action}
                    hasMatches={listingStatus(pendingRegistration) === "MATCHED"}
                    errorMessage={t(matching.errorKey)}
                    failed={endFailed}
                    onCancel={() => {
                        if (!matching.busy) setPendingAction(undefined);
                    }}
                    onConfirm={() => void confirmAction()}
                />
            )}
        </main>
    );
}
