import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import {
    Autocomplete,
    Avatar,
    Button,
    Card,
    Checkbox,
    DateRangeCalendar,
    Icon,
    IconChoiceGroup,
    RangeSelector,
    Spinner,
    TextField,
    Tooltip,
} from "@/components/ui";
import { MatchingFilterPopover } from "./MatchingFilterPopover";
import { RecruitmentStatusBadge } from "./RecruitmentStatusBadge";
import { RecruitmentUpdatedDate } from "./RecruitmentUpdatedDate";
import { RecruitmentDetailsPanel } from "./RecruitmentDetailsPanel";
import type { CandidateQuery, CandidateResult, MatchingDestination } from "@/hooks/matching.types";

type CompanionSearchProps = {
    destinations: readonly MatchingDestination[];
    result: CandidateResult | null;
    busy: boolean;
    failed: boolean;
    loading: boolean;
    catalogFailed: boolean;
    signedIn: boolean;
    onLogin: () => void;
    onReload: () => void;
    onSearch: (query: CandidateQuery) => Promise<boolean>;
    onDetailsChange: (open: boolean) => void;
};

export function CompanionSearch({
    destinations,
    result,
    busy,
    failed,
    loading,
    catalogFailed,
    signedIn,
    onLogin,
    onReload,
    onSearch,
    onDetailsChange,
}: CompanionSearchProps) {
    const { t, i18n } = useTranslation();
    const [destinationId, setDestinationId] = useState("");
    const [query, setQuery] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [keyword, setKeyword] = useState("");
    const [gender, setGender] = useState("");
    const [anyAge, setAnyAge] = useState(true);
    const [ages, setAges] = useState<[number, number]>([20, 40]);
    const [submitted, setSubmitted] = useState<CandidateQuery | null>(null);
    const [selectedId, setSelectedId] = useState<number>();
    const selectedCandidate = result?.candidates.find((item) => item.searchId === selectedId);
    const selected = destinations.find((item) => String(item.id) === destinationId);
    const valid = Boolean(selected && startDate && endDate && startDate <= endDate);
    useEffect(() => {
        onDetailsChange(Boolean(selectedCandidate));
        return () => onDetailsChange(false);
    }, [selectedCandidate, onDetailsChange]);
    const date = (value: string) =>
        new Intl.DateTimeFormat(i18n.resolvedLanguage === "en" ? "en-US" : "ko-KR", {
            month: "short",
            day: "numeric",
            timeZone: "UTC",
        }).format(new Date(`${value}T00:00:00Z`));
    async function search(request: CandidateQuery): Promise<void> {
        const ok = await onSearch(request);
        if (ok) {
            setSubmitted(request);
            setSelectedId(undefined);
        }
    }
    function reset(): void {
        setDestinationId("");
        setQuery("");
        setStartDate("");
        setEndDate("");
        setKeyword("");
        setGender("");
        setAnyAge(true);
        setSelectedId(undefined);
        setSubmitted(null);
    }
    return (
        <section
            className="companion-search companion-search--compact"
            aria-label={t("matchingEntry.searchTitle")}
        >
            {!signedIn ? (
                <div className="matching-empty">
                    <p>{t("matchingFlow.loginHint")}</p>
                    <Button onClick={onLogin}>{t("matchingFlow.login")}</Button>
                </div>
            ) : (
                <>
                    <header className="matching-recruitments__heading">
                        <h2>
                            <Icon name="search" />
                            {t("matchingEntry.searchTitle")}
                        </h2>
                    </header>
                    <form
                        className="matching-recruitments__filters companion-search__filter-row"
                        onSubmit={(event) => {
                            event.preventDefault();
                            if (!valid || busy) return;
                            void search({
                                destinationId: Number(destinationId),
                                startDate,
                                endDate,
                                ...(keyword.trim() ? { q: keyword.trim() } : {}),
                                ...(gender === "MALE" || gender === "FEMALE"
                                    ? { preferredGender: gender }
                                    : {}),
                                ...(!anyAge
                                    ? { preferredMinAge: ages[0], preferredMaxAge: ages[1] }
                                    : {}),
                                page: 0,
                                size: 9,
                            });
                        }}
                    >
                        <fieldset disabled={busy || loading || catalogFailed}>
                            <div className="matching-filter-search">
                                <Icon name="search" />
                                <TextField
                                    label={t("matchingFlow.keyword")}
                                    placeholder={t("matchingFlow.titleSearchPlaceholder")}
                                    value={keyword}
                                    maxLength={80}
                                    onChange={(event) => setKeyword(event.target.value)}
                                />
                            </div>
                            <div className="companion-search__destination">
                                <Autocomplete
                                    label={t("matchingFlow.destinationSearch")}
                                    value={destinationId}
                                    query={query}
                                    options={destinations.map((item) => ({
                                        value: String(item.id),
                                        label: item.name,
                                    }))}
                                    emptyLabel={t("matchingFlow.noDestinations")}
                                    placeholder={t("matchingFlow.destinationPlaceholder")}
                                    onQueryChange={(value) => {
                                        setQuery(value);
                                        setDestinationId("");
                                    }}
                                    onSelect={(option) => {
                                        setDestinationId(option.value);
                                        setQuery(option.label);
                                    }}
                                />
                            </div>
                            <MatchingFilterPopover
                                className="matching-filter-date-trigger"
                                icon="calendar"
                                label={
                                    startDate
                                        ? `${date(startDate)} ~ ${endDate ? date(endDate) : "…"}`
                                        : t("matchingFlow.period")
                                }
                            >
                                <DateRangeCalendar
                                    label={t("matchingFlow.period")}
                                    startDate={startDate}
                                    endDate={endDate}
                                    onChange={(start, end) => {
                                        setStartDate(start);
                                        setEndDate(end);
                                    }}
                                />
                            </MatchingFilterPopover>
                            <MatchingFilterPopover
                                icon="journey-age"
                                label={
                                    anyAge ? t("matchingFlow.ageFilter") : `${ages[0]} ~ ${ages[1]}`
                                }
                            >
                                <Checkbox
                                    label={t("matchingFlow.anyAge")}
                                    checked={anyAge}
                                    onChange={(event) => setAnyAge(event.target.checked)}
                                />
                                <RangeSelector
                                    label={t("matchingFlow.ageFilter")}
                                    lowerLabel={t("matchingFlow.minAge")}
                                    upperLabel={t("matchingFlow.maxAge")}
                                    lowerValueLabel={String(ages[0])}
                                    upperValueLabel={String(ages[1])}
                                    min={19}
                                    max={100}
                                    value={ages}
                                    ticks={[19, 30, 40, 50, 60, 70, 80, 90, 100]}
                                    disabled={anyAge}
                                    onChange={setAges}
                                />
                            </MatchingFilterPopover>
                            <IconChoiceGroup
                                label={t("matchingFlow.gender")}
                                value={gender}
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
                                onChange={setGender}
                            />
                            <Button
                                variant="plain"
                                className="matching-text-action"
                                onClick={reset}
                            >
                                {t("matchingFlow.resetFilters")}
                            </Button>
                            <Tooltip content={t("matchingEntry.searchAction")}>
                                <Button
                                    variant="plain"
                                    type="submit"
                                    className="companion-search__icon-submit"
                                    aria-label={t("matchingEntry.searchAction")}
                                    aria-busy={busy || undefined}
                                    disabled={!valid || busy}
                                >
                                    <Icon name="search" />
                                </Button>
                            </Tooltip>
                        </fieldset>
                    </form>
                    {loading && <Spinner label={t("matchingFlow.loading")} />}
                    {catalogFailed && (
                        <div role="alert" className="matching-error">
                            <p>{t("matchingFlow.catalogError")}</p>
                            <Button
                                variant="plain"
                                className="matching-text-action"
                                onClick={onReload}
                            >
                                {t("matchingFlow.retry")}
                            </Button>
                        </div>
                    )}
                    {failed && (
                        <p className="matching-error" role="alert">
                            {t("matchingEntry.searchError")}
                        </p>
                    )}
                    {!result || !submitted || result.candidates.length === 0 ? (
                        <div className="matching-filter-empty" role="status">
                            <div className="matching-filter-empty__illustration" aria-hidden="true">
                                <Icon name="search" />
                            </div>
                            <h3>
                                {t(
                                    submitted
                                        ? "matchingFlow.noFilteredPosts"
                                        : "matchingFlow.chooseSearchConditions",
                                )}
                            </h3>
                            {submitted && <p>{t("matchingFlow.adjustFilters")}</p>}
                        </div>
                    ) : (
                        <>
                            <div className="matching-registration-grid" aria-busy={busy}>
                                {result.candidates.map((candidate) => (
                                    <Card
                                        key={candidate.searchId}
                                        className={`matching-registration${selectedId === candidate.searchId ? " is-selected" : ""}`}
                                    >
                                        <div>
                                            <div className="recruitment-heading-row">
                                                <RecruitmentStatusBadge status="ACTIVE" />
                                                <RecruitmentUpdatedDate
                                                    updatedAt={candidate.updatedAt}
                                                />
                                            </div>
                                            <h3>
                                                <Button
                                                    variant="plain"
                                                    className="matching-registration__open"
                                                    aria-expanded={
                                                        selectedId === candidate.searchId
                                                    }
                                                    aria-controls={
                                                        selectedId === candidate.searchId
                                                            ? "recruitment-details-panel"
                                                            : undefined
                                                    }
                                                    onClick={() =>
                                                        setSelectedId((current) =>
                                                            current === candidate.searchId
                                                                ? undefined
                                                                : candidate.searchId,
                                                        )
                                                    }
                                                >
                                                    {candidate.title || t("matchingFlow.untitled")}
                                                </Button>
                                            </h3>
                                            <p>
                                                <Icon name="pin" />
                                                {
                                                    destinations.find(
                                                        (item) =>
                                                            item.id === submitted.destinationId,
                                                    )?.name
                                                }
                                            </p>
                                            <p>
                                                <Icon name="calendar" />
                                                {t("matchingFlow.dateRange", {
                                                    start: date(candidate.startDate),
                                                    end: date(candidate.endDate),
                                                })}
                                            </p>
                                        </div>
                                        <div className="companion-search__card-footer">
                                            <span className="companion-search__person">
                                                <Avatar src={candidate.profileImageUrl} />
                                                {candidate.nickname}
                                            </span>
                                            <span>
                                                {t("matchingFlow.overlap", {
                                                    count: candidate.overlapDays,
                                                })}
                                            </span>
                                        </div>
                                    </Card>
                                ))}
                            </div>
                            <nav
                                className="matching-pagination"
                                aria-label={t("matchingFlow.results")}
                            >
                                <Button
                                    variant="plain"
                                    className="matching-text-action"
                                    disabled={busy || (submitted.page ?? 0) === 0}
                                    onClick={() =>
                                        void search({
                                            ...submitted,
                                            page: (submitted.page ?? 0) - 1,
                                        })
                                    }
                                >
                                    {t("matchingFlow.previousPage")}
                                </Button>
                                <span>{(submitted.page ?? 0) + 1}</span>
                                <Button
                                    variant="plain"
                                    className="matching-text-action"
                                    disabled={busy || !result.hasNext}
                                    onClick={() =>
                                        void search({
                                            ...submitted,
                                            page: (submitted.page ?? 0) + 1,
                                        })
                                    }
                                >
                                    {t("matchingFlow.nextPage")}
                                </Button>
                            </nav>
                        </>
                    )}
                    <RecruitmentDetailsPanel
                        details={
                            selectedCandidate && submitted
                                ? {
                                      candidate: selectedCandidate,
                                      destination:
                                          destinations.find(
                                              (item) => item.id === submitted.destinationId,
                                          )?.name ?? "",
                                      onClose: () => setSelectedId(undefined),
                                      busy: busy,
                                      failed: false,
                                  }
                                : undefined
                        }
                    />
                </>
            )}
        </section>
    );
}
