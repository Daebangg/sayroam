import { useRef, useState } from "react";
import { api } from "@/api/api";
import { ApiError } from "@/api/apiError";

export type JoinRoomStatus = "idle" | "joining" | "joined" | "notFound" | "expired" | "failed";

type JoinRoom = { status: JoinRoomStatus; join: (code: string) => Promise<void> };

/** 초대 코드로 방에 참여한다. 링크를 열기만 해서 참여되지 않도록 회원이 누를 때만 보낸다. */
export function useJoinRoom(): JoinRoom {
    const [status, setStatus] = useState<JoinRoomStatus>("idle");
    const pending = useRef(false);

    async function join(code: string): Promise<void> {
        if (pending.current) return;
        pending.current = true;
        setStatus("joining");
        try {
            await api.call("joinByInvitation", { body: { invitationCode: code } });
            setStatus("joined");
        } catch (error) {
            const httpStatus = error instanceof ApiError ? error.status : undefined;
            setStatus(httpStatus === 404 ? "notFound" : httpStatus === 410 ? "expired" : "failed");
        } finally {
            pending.current = false;
        }
    }

    return { status, join };
}
