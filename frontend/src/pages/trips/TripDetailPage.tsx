import { useMemo, useRef, useState } from "react";
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
import { useTripChat } from "@/hooks/api/chat/useTripChat";
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
    return (
        member && (
            <TripDetail
                key={`${member.userId}-${tripId}`}
                tripId={tripId}
                myUserId={member.userId}
            />
        )
    );
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

function TripDetail({ tripId, myUserId }: { tripId: number; myUserId: number }) {
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
    return <TripPlanner trip={detail.trip} detail={detail} myUserId={myUserId} />;
}

type Picking = { location: PickedLocation | null };
type Notice = { id: number; message: string; tone: ToastTone };
type Invite = { link: string | null; loading: boolean; failed: boolean };

type TripPlannerProps = {
    trip: MyTrip;
    detail: ReturnType<typeof useTripDetail>;
    myUserId: number;
};

function TripPlanner({ trip, detail, myUserId }: TripPlannerProps) {
    const { t } = useTranslation();
    const { places } = detail;
    // 지도 마커를 다시 그리고 범위를 맞추는 기준이므로 입력 등 다른 상태로 다시 렌더링될 때 새로 만들지 않는다.
    const sections = useMemo(
        () => tripSections(trip.startDate, trip.endDate, places),
        [trip.startDate, trip.endDate, places],
    );
    // 이름을 바꾸는 등 여행 정보만 갱신될 때 지도가 다시 여행지 영역으로 돌아가지 않게 영역 값이 같으면 유지한다.
    const destination = useMemo(
        () => ({
            south: trip.boundsSouth,
            west: trip.boundsWest,
            north: trip.boundsNorth,
            east: trip.boundsEast,
        }),
        [trip.boundsSouth, trip.boundsWest, trip.boundsNorth, trip.boundsEast],
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
    const [invite, setInvite] = useState<Invite>({ link: null, loading: false, failed: false });
    const search = usePlaceSearch(addingKey === null ? "" : query);
    // 패널을 닫아도 연결과 받은 메시지를 유지하도록 페이지에서 연결한다. 채팅방이 없으면 연결하지 않는다.
    const chat = useTripChat(trip.roomId, myUserId);
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

    // 추가 상자를 열고 닫을 때마다 바뀐다. 추가가 끝났을 때 그 사이 다른 일차의 상자를 열었으면 닫지 않는다.
    const addSession = useRef(0);

    function openAdd(key: string) {
        addSession.current += 1;
        setAddingKey(key);
        setQuery("");
        setPicking(null);
    }

    function closeAdd() {
        addSession.current += 1;
        setAddingKey(null);
        setQuery("");
        setPicking(null);
    }

    // 열 때마다 서버에 물어 만료된 링크를 보여 주지 않는다. 받는 중이면 다시 요청하지 않는다.
    async function loadInvite() {
        if (invite.loading) return;
        setInvite({ link: null, loading: true, failed: false });
        const code = await detail.loadInvitation();
        setInvite({
            link: code ? invitationUrl(window.location.origin, code) : null,
            loading: false,
            failed: code === null,
        });
    }

    function toggleChat() {
        setPanel(panel === "chat" ? null : "chat");
    }

    function openInvite() {
        setPanel("invite");
        void loadInvite();
    }

    function toggleInvite() {
        if (panel === "invite") setPanel(null);
        else openInvite();
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

    async function finishAdd(request: Promise<boolean | null>, name: string) {
        const session = addSession.current;
        const added = await request;
        if (added === null) return;
        if (!added) return notify(t("tripDetail.addFailed"), "error");
        notify(t("tripDetail.placeAdded", { name }));
        if (addSession.current === session) closeAdd();
    }

    function addSearched(section: TripSection, place: SearchedPlace) {
        return finishAdd(detail.addPlace(place.sourceId, section.day), place.name);
    }

    function addCustom(section: TripSection, location: PickedLocation, name: string) {
        return finishAdd(detail.addCustomPlace(location, name, section.day), name);
    }

    return (
        <main id="main-content" tabIndex={-1} className="trip-detail">
            <TripDayDock
                sections={sections}
                activeKey={activeKey}
                onSelect={goToSection}
                chatOpen={panel === "chat"}
                onToggleChat={toggleChat}
                inviteOpen={panel === "invite"}
                onToggleInvite={toggleInvite}
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
                    onStartAdd={openAdd}
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
                destination={destination}
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
                    link={invite.link}
                    loading={invite.loading}
                    failed={invite.failed}
                    onRetry={() => void loadInvite()}
                    onCopy={() => void copyInvite()}
                    onClose={() => setPanel(null)}
                />
            )}
            {panel === "chat" && (
                <TripChatPanel
                    title={trip.title ?? t("trips.untitled")}
                    myUserId={myUserId}
                    status={chat.status}
                    connection={chat.connection}
                    messages={chat.messages}
                    outgoing={chat.outgoing}
                    members={chat.members}
                    syncFailed={chat.syncFailed}
                    onSend={chat.send}
                    onRetry={chat.retry}
                    onDiscard={chat.discard}
                    onRetrySync={chat.retrySync}
                    onReload={chat.reload}
                    onInvite={openInvite}
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
