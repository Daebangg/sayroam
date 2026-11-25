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
    /** 지금 쓸 수 있는 초대 코드. 없거나 만료됐으면 새로 발급한다. 실패하면 null */
    loadInvitation: () => Promise<string | null>;
};

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
            // 이름 변경 응답은 ID와 저장된 이름만 담으므로 나머지 여행 정보는 유지한다.
            setTrip((current) => (current ? { ...current, title: updated.title } : current));
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

    // 보기만 하는 조회는 잠그지 않는다. 링크가 없을 때만 발급을 요청해 서버가 잠근다.
    async function loadInvitation(): Promise<string | null> {
        const params = { path: { tripId } };
        try {
            const { code } = await api.call("getTripInvitation", { params });
            if (code) return code;
            return (await api.call("issueTripInvitation", { params })).code;
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
        loadInvitation,
    };
}
