import { useEffect, useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";
import type { MyTrip, PickedLocation, TripPlace } from "../../trip.types";

export type TripDetailStatus = "loading" | "ready" | "notFound" | "failed";
export type RenameResult = "saved" | "tooLong" | "failed";

type TripDetail = {
    trip: MyTrip | null;
    places: TripPlace[];
    status: TripDetailStatus;
    /** 장소를 추가하는 중이면 true. 추가 요청은 하나씩만 보낸다. */
    adding: boolean;
    reload: () => void;
    rename: (title: string) => Promise<RenameResult>;
    /** 추가했으면 true, 실패하면 false, 이미 다른 추가가 진행 중이라 보내지 않았으면 null */
    addPlace: (sourceId: string, day: number | null) => Promise<boolean | null>;
    addCustomPlace: (
        location: PickedLocation,
        name: string,
        day: number | null,
    ) => Promise<boolean | null>;
    /** 초대 코드를 새로 발급한다. 채팅방이 없는 여행이면 채팅방을 먼저 만들어 연결한다. 실패하면 null */
    createInvitation: () => Promise<string | null>;
};

// 방 이름은 서버 기준 20자(코드 포인트)까지다. 더 긴 여행 이름은 앞부분만 쓴다.
const ROOM_NAME_MAX_LENGTH = 20;

/** 여행이 바뀌면 호출하는 쪽에서 key로 다시 마운트해 이전 여행의 상태를 남기지 않는다. */
export function useTripDetail(tripId: number): TripDetail {
    const [trip, setTrip] = useState<MyTrip | null>(null);
    const [places, setPlaces] = useState<TripPlace[]>([]);
    const [status, setStatus] = useState<TripDetailStatus>("loading");
    const [revision, setRevision] = useState(0);
    const [adding, setAdding] = useState(false);
    const pending = useRef(false);

    useEffect(() => {
        const controller = new AbortController();
        const params = { path: { tripId } };
        void Promise.all([
            api.call("getTrip", { params, signal: controller.signal }),
            api.call("listTripPlaces", { params, signal: controller.signal }),
        ])
            .then(([found, list]) => {
                if (controller.signal.aborted) return;
                setTrip(found);
                setPlaces(list.places);
                setStatus("ready");
            })
            .catch((error: unknown) => {
                if (controller.signal.aborted) return;
                setStatus(
                    error instanceof ApiError && error.status === 404 ? "notFound" : "failed",
                );
            });
        return () => controller.abort();
    }, [tripId, revision]);

    function reload(): void {
        setStatus("loading");
        setRevision((value) => value + 1);
    }

    async function rename(title: string): Promise<RenameResult> {
        try {
            const updated = await api.call("updateTrip", {
                params: { path: { tripId } },
                body: { title },
            });
            setTrip(updated);
            return "saved";
        } catch (error) {
            return error instanceof ApiError && error.problem?.code === "TRIP_TITLE_TOO_LONG"
                ? "tooLong"
                : "failed";
        }
    }

    // 추가는 반복하면 같은 장소가 여러 번 담기므로 목록을 다시 받을 때까지 다시 보내지 않고 자동 재시도도 하지 않는다.
    async function add(send: () => Promise<unknown>): Promise<boolean | null> {
        if (pending.current) return null;
        pending.current = true;
        setAdding(true);
        try {
            try {
                await send();
            } catch {
                return false;
            }
            try {
                const list = await api.call("listTripPlaces", { params: { path: { tripId } } });
                setPlaces(list.places);
            } catch {
                // 추가는 끝났으므로 목록 갱신이 실패해도 성공으로 알리고 다음 조회에서 맞춘다.
            }
            return true;
        } finally {
            // 추가 뒤 목록 재조회까지 끝나야 잠금을 푼다. 그 전에 같은 결과를 다시 눌러도 보내지 않는다.
            pending.current = false;
            setAdding(false);
        }
    }

    function addPlace(sourceId: string, day: number | null): Promise<boolean | null> {
        return add(() =>
            api.call("addTripPlace", { params: { path: { tripId } }, body: { sourceId, day } }),
        );
    }

    function addCustomPlace(
        location: PickedLocation,
        name: string,
        day: number | null,
    ): Promise<boolean | null> {
        return add(() =>
            api.call("addTripCustomPlace", {
                params: { path: { tripId } },
                body: { ...location, name, day },
            }),
        );
    }

    async function linkedRoomId(): Promise<number> {
        if (trip?.roomId) return trip.roomId;
        const name = trip?.title ? [...trip.title].slice(0, ROOM_NAME_MAX_LENGTH).join("") : null;
        try {
            const { roomId } = await api.call("createTripRoom", {
                params: { path: { tripId } },
                body: { name },
            });
            setTrip((current) => (current ? { ...current, roomId } : current));
            return roomId;
        } catch (error) {
            // 다른 요청이 먼저 채팅방을 연결했으면 여행을 다시 조회해 그 방을 쓴다.
            if (!(error instanceof ApiError && error.status === 409)) throw error;
            const found = await api.call("getTrip", { params: { path: { tripId } } });
            setTrip(found);
            if (found.roomId === null) throw error;
            return found.roomId;
        }
    }

    async function createInvitation(): Promise<string | null> {
        try {
            const roomId = await linkedRoomId();
            const { code } = await api.call("createInvitation", { params: { path: { roomId } } });
            return code;
        } catch {
            return null;
        }
    }

    return {
        trip,
        places,
        status,
        adding,
        reload,
        rename,
        addPlace,
        addCustomPlace,
        createInvitation,
    };
}
