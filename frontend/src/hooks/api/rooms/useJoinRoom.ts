import { useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";

export type JoinRoomStatus = "idle" | "joining" | "joined" | "notFound" | "expired" | "failed";

type JoinRoom = {
    status: JoinRoomStatus;
    /** 참여한 방에 연결된 여행. 여행이 없는 방이거나 찾지 못했으면 null */
    tripId: number | null;
    join: (code: string) => Promise<void>;
};

// 참여 응답은 방 ID만 담는다. 방이 여행을 모르는 구조라 내 여행 목록에서 같은 방의 여행을 찾는다.
async function findTripOfRoom(roomId: number): Promise<number | null> {
    try {
        const { trips } = await api.call("listMyTrips");
        return trips.find((trip) => trip.roomId === roomId)?.tripId ?? null;
    } catch {
        // 참여는 끝났으므로 여행을 찾지 못해도 참여 결과는 그대로 알린다.
        return null;
    }
}

/** 초대 코드로 방에 참여한다. 링크를 열기만 해서 참여되지 않도록 회원이 누를 때만 보낸다. */
export function useJoinRoom(): JoinRoom {
    const [status, setStatus] = useState<JoinRoomStatus>("idle");
    const [tripId, setTripId] = useState<number | null>(null);
    const pending = useRef(false);

    async function join(code: string): Promise<void> {
        if (pending.current) return;
        pending.current = true;
        setStatus("joining");
        try {
            const { roomId } = await api.call("joinByInvitation", {
                body: { invitationCode: code },
            });
            setTripId(await findTripOfRoom(roomId));
            setStatus("joined");
        } catch (error) {
            const httpStatus = error instanceof ApiError ? error.status : undefined;
            setStatus(httpStatus === 404 ? "notFound" : httpStatus === 410 ? "expired" : "failed");
        } finally {
            pending.current = false;
        }
    }

    return { status, tripId, join };
}
