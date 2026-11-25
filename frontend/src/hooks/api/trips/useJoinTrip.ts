import { useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";

/** invalid는 없거나 만료된 초대 코드다. 서버가 둘을 구분하지 않는다. */
export type JoinTripStatus = "idle" | "joining" | "joined" | "invalid" | "failed";

type JoinTrip = {
    status: JoinTripStatus;
    /** 참여한 여행. 참여하기 전이면 null */
    tripId: number | null;
    join: (code: string) => Promise<void>;
};

/** 초대 코드로 여행에 참여한다. 링크를 열기만 해서 참여되지 않도록 회원이 누를 때만 보낸다. */
export function useJoinTrip(): JoinTrip {
    const [status, setStatus] = useState<JoinTripStatus>("idle");
    const [tripId, setTripId] = useState<number | null>(null);
    const pending = useRef(false);

    async function join(code: string): Promise<void> {
        if (pending.current) return;
        pending.current = true;
        setStatus("joining");
        try {
            const joined = await api.call("joinTrip", { body: { invitationCode: code } });
            setTripId(joined.tripId);
            setStatus("joined");
        } catch (error) {
            setStatus(error instanceof ApiError && error.status === 404 ? "invalid" : "failed");
        } finally {
            pending.current = false;
        }
    }

    return { status, tripId, join };
}
