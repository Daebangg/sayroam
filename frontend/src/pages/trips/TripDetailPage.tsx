import { useMemo, useState } from "react";
import { useOutletContext, useParams } from "react-router";
import { useTranslation } from "react-i18next";
import { ButtonLink, Button, PageState, Toast, type ToastTone } from "@/components/ui";
import { PlaceAddBox } from "@/components/features/travel/PlaceAddBox";
import { TripChatPanel } from "@/components/features/travel/TripChatPanel";
import { TripDayDock } from "@/components/features/travel/TripDayDock";
import { TripInvitePanel } from "@/components/features/travel/TripInvitePanel";
import { TripItinerary } from "@/components/features/travel/TripItinerary";
import { TripMap } from "@/components/features/travel/TripMap";
import { TripMapLayers } from "@/components/features/travel/TripMapLayers";
import { TripTitleEditor } from "@/components/features/travel/TripTitleEditor";
import {
    tripPlaceId,
    tripSectionId,
    tripSections,
    type TripSection,
} from "@/components/features/travel/tripDays";
import { PLACE_QUERY_MAX_LENGTH, usePlaceSearch } from "@/hooks/api/trips/usePlaceSearch";
import { useTripDetail } from "@/hooks/api/trips/useTripDetail";
import type { MyTrip, PickedLocation, SearchedPlace } from "@/hooks/trip.types";
import type { PageContext } from "@/routing/context";
import { invitationUrl, pagePaths } from "@/routing/paths";

// 서버 검증과 같은 기준. 서버가 최종 판단하며 화면은 입력 길이만 맞춘다.
const TRIP_TITLE_MAX_LENGTH = 30;
const CUSTOM_PLACE_NAME_MAX_LENGTH = 100;

function parseTripId(value: string | undefined): number | null {
    if (!value || !/^[1-9]\d{0,15}$/.test(value)) return null;
    const id = Number(value);
    return Number.isSafeInteger(id) ? id : null;
}

// 회원 전용 경로라 App의 MemberGate가 로그인 전 진입을 막는다.
export function TripDetailPage() {
    const { member } = useOutletContext<PageContext>();
    const tripId = parseTripId(useParams().tripId);
    if (tripId === null) return <TripNotFound />;
    return member && <TripDetail key={`${member.userId}-${tripId}`} tripId={tripId} />;
}

function TripNotFound() {
    const { t } = useTranslation();
    return (
        <main id="main-content" tabIndex={-1} className="workspace-page">
            <PageState
                kind="notFound"
                title={t("tripDetail.notFoundTitle")}
                description={t("tripDetail.notFoundDescription")}
                actions={
                    <ButtonLink to={pagePaths.trips} variant="outline">
                        {t("tripDetail.backToTrips")}
                    </ButtonLink>
                }
            />
        </main>
    );
}

function TripDetail({ tripId }: { tripId: number }) {
    const { t } = useTranslation();
    const detail = useTripDetail(tripId);
    if (detail.status === "notFound") return <TripNotFound />;
    if (detail.status === "loading" || !detail.trip)
        return (
            <main id="main-content" tabIndex={-1} className="workspace-page">
                {detail.status === "failed" ? (
                    <PageState
                        kind="error"
                        title={t("tripDetail.loadErrorTitle")}
                        description={t("tripDetail.loadErrorDescription")}
                        actions={<Button onClick={detail.reload}>{t("tripDetail.retry")}</Button>}
                    />
                ) : (
                    <PageState
                        kind="loading"
                        title={t("tripDetail.loading")}
                        description={t("tripDetail.loading")}
                    />
                )}
            </main>
        );
    return <TripPlanner trip={detail.trip} detail={detail} />;
}

type Picking = { location: PickedLocation | null };
type Notice = { id: number; message: string; tone: ToastTone };
type Invite = { link: string | null; creating: boolean; failed: boolean };

function TripPlanner({ trip, detail }: { trip: MyTrip; detail: ReturnType<typeof useTripDetail> }) {
    const { t } = useTranslation();
    const { places } = detail;
    // 지도 마커를 다시 그리고 범위를 맞추는 기준이므로 입력 등 다른 상태로 다시 렌더링될 때 새로 만들지 않는다.
    const sections = useMemo(
        () => tripSections(trip.startDate, trip.endDate, places),
        [trip.startDate, trip.endDate, places],
    );
    const [hidden, setHidden] = useState<ReadonlySet<string>>(() => new Set());
    const visible = useMemo(
        () => new Set(sections.map((section) => section.key).filter((key) => !hidden.has(key))),
        [sections, hidden],
    );
    const [activeKey, setActiveKey] = useState(sections[0]?.key ?? "");
    const [highlighted, setHighlighted] = useState<number | null>(null);
    const [focus, setFocus] = useState<{ placeId: number; seq: number } | null>(null);
    const [addingKey, setAddingKey] = useState<string | null>(null);
    const [query, setQuery] = useState("");
    const [picking, setPicking] = useState<Picking | null>(null);
    const [notice, setNotice] = useState<Notice | null>(null);
    const [panel, setPanel] = useState<"chat" | "invite" | null>(null);
    const [invite, setInvite] = useState<Invite>({ link: null, creating: false, failed: false });
    const search = usePlaceSearch(addingKey === null ? "" : query);
    const addingSection = sections.find((section) => section.key === addingKey);

    function notify(message: string, tone: ToastTone = "success") {
        setNotice((previous) => ({ id: (previous?.id ?? 0) + 1, message, tone }));
    }

    function showOnMap(placeId: number) {
        const section = sections.find((item) =>
            item.places.some((place) => place.tripPlaceId === placeId),
        );
        if (section && hidden.has(section.key)) {
            const next = new Set(hidden);
            next.delete(section.key);
            setHidden(next);
        }
        setHighlighted(placeId);
        setFocus((previous) => ({ placeId, seq: (previous?.seq ?? 0) + 1 }));
    }

    function revealInList(placeId: number) {
        setHighlighted(placeId);
        document
            .getElementById(tripPlaceId(placeId))
            ?.scrollIntoView({ behavior: "smooth", block: "center" });
    }

    function goToSection(key: string) {
        setActiveKey(key);
        document
            .getElementById(tripSectionId(key))
            ?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function closeAdd() {
        setAddingKey(null);
        setQuery("");
        setPicking(null);
    }

    async function createInvite() {
        if (invite.creating) return;
        setInvite((current) => ({ ...current, creating: true, failed: false }));
        const code = await detail.createInvitation();
        setInvite((current) => ({
            link: code ? invitationUrl(window.location.origin, code) : current.link,
            creating: false,
            failed: code === null,
        }));
    }

    async function copyInvite() {
        if (!invite.link) return;
        try {
            await navigator.clipboard.writeText(invite.link);
            notify(t("tripDetail.inviteCopied"));
        } catch {
            notify(t("tripDetail.inviteCopyFailed"), "error");
        }
    }

    async function addSearched(section: TripSection, place: SearchedPlace) {
        const added = await detail.addPlace(place.sourceId, section.day);
        if (!added) return notify(t("tripDetail.addFailed"), "error");
        notify(t("tripDetail.placeAdded", { name: place.name }));
        closeAdd();
    }

    async function addCustom(section: TripSection, location: PickedLocation, name: string) {
        const added = await detail.addCustomPlace(location, name, section.day);
        if (!added) return notify(t("tripDetail.addFailed"), "error");
        notify(t("tripDetail.placeAdded", { name }));
        closeAdd();
    }

    return (
        <main id="main-content" tabIndex={-1} className="trip-detail">
            <TripDayDock
                sections={sections}
                activeKey={activeKey}
                onSelect={goToSection}
                chatOpen={panel === "chat"}
                onToggleChat={() => setPanel((current) => (current === "chat" ? null : "chat"))}
                inviteOpen={panel === "invite"}
                onToggleInvite={() =>
                    setPanel((current) => (current === "invite" ? null : "invite"))
                }
            />
            <div className="trip-detail__panel">
                <TripTitleEditor
                    title={trip.title}
                    maxLength={TRIP_TITLE_MAX_LENGTH}
                    onSave={async (title) => {
                        const result = await detail.rename(title);
                        if (result === "saved") notify(t("tripDetail.titleSaved"));
                        return result;
                    }}
                />
                <TripItinerary
                    sections={sections}
                    highlightedPlaceId={highlighted}
                    onFocusPlace={showOnMap}
                    onActiveChange={setActiveKey}
                    addingKey={addingKey}
                    onStartAdd={(key) => {
                        setAddingKey(key);
                        setQuery("");
                        setPicking(null);
                    }}
                    addBox={
                        addingSection && (
                            <PlaceAddBox
                                query={query}
                                queryMaxLength={PLACE_QUERY_MAX_LENGTH}
                                nameMaxLength={CUSTOM_PLACE_NAME_MAX_LENGTH}
                                onQueryChange={setQuery}
                                search={search}
                                adding={detail.adding}
                                onAddPlace={(place) => void addSearched(addingSection, place)}
                                picking={picking !== null}
                                picked={picking?.location ?? null}
                                onStartPicking={() => setPicking({ location: null })}
                                onCancelPicking={() => setPicking(null)}
                                onAddCustom={(name) => {
                                    if (picking?.location)
                                        void addCustom(addingSection, picking.location, name);
                                }}
                                onClose={closeAdd}
                            />
                        )
                    }
                />
            </div>
            <TripMap
                sections={sections}
                visible={visible}
                focus={focus}
                onSelectPlace={revealInList}
                picking={picking !== null}
                picked={picking?.location ?? null}
                onPick={(location) => setPicking({ location })}
            >
                <TripMapLayers
                    sections={sections}
                    visible={visible}
                    onChange={(next) =>
                        setHidden(
                            new Set(
                                sections
                                    .map((section) => section.key)
                                    .filter((key) => !next.has(key)),
                            ),
                        )
                    }
                />
                {picking !== null && (
                    <p className="trip-map__hint" role="status">
                        {t("tripDetail.pickHint")}
                    </p>
                )}
            </TripMap>
            {panel === "invite" && (
                <TripInvitePanel
                    hasRoom={trip.roomId !== null}
                    link={invite.link}
                    creating={invite.creating}
                    failed={invite.failed}
                    onCreate={() => void createInvite()}
                    onCopy={() => void copyInvite()}
                    onClose={() => setPanel(null)}
                />
            )}
            {panel === "chat" && (
                <TripChatPanel
                    title={trip.title ?? t("trips.untitled")}
                    onClose={() => setPanel(null)}
                />
            )}
            {notice && (
                <Toast
                    key={notice.id}
                    message={notice.message}
                    tone={notice.tone}
                    onDismiss={() => setNotice(null)}
                />
            )}
        </main>
    );
}
