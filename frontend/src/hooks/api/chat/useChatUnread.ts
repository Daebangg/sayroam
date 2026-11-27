import { useEffect, useRef, useState } from "react";
import { api } from "@/api/api";
import type { ChatMessage, TripChatStatus } from "../../chat.types";
import { createReadPositionSender, type ReadPositionSender } from "./readPositionSender";
import { latestMessageId, unreadCount, type UnreadBaseline } from "./unreadCount";

const READ_IDLE_MS = 1000;
const READ_MAX_WAIT_MS = 5000;

/** 다른 방의 값이 남지 않도록 방 ID와 함께 둔다. */
type ForRoom<T> = { roomId: number; value: T };

/**
 * 채팅방의 안 읽은 메시지 개수.
 * 놓친 메시지를 다 받은 뒤 서버 개수를 받고, 그 뒤 도착한 다른 회원의 메시지를 더한다.
 * 패널을 열어 보는 동안(reading)은 0이고, 본 마지막 메시지를 모아서 서버에 읽은 위치로 기록한다.
 * 패널을 닫거나 화면을 벗어나거나 탭이 가려지면 기다리지 않고 보낸다.
 */
export function useChatUnread(
    roomId: number | null,
    status: TripChatStatus,
    messages: ChatMessage[],
    myUserId: number,
    reading: boolean,
): number {
    const latestId = latestMessageId(messages);
    const ready = roomId !== null && status === "ready";
    const [baseline, setBaseline] = useState<ForRoom<UnreadBaseline> | null>(null);
    const [readUntil, setReadUntil] = useState<ForRoom<number> | null>(null);
    const [wasReading, setWasReading] = useState(reading);
    const latest = useRef(latestId);
    const sender = useRef<ReadPositionSender | null>(null);

    // 패널을 닫으면 그때까지 본 메시지 다음부터 센다.
    if (wasReading !== reading) {
        setWasReading(reading);
        if (!reading && roomId !== null) setReadUntil({ roomId, value: latestId });
    }

    useEffect(() => {
        latest.current = latestId;
    }, [latestId]);

    // 놓친 메시지를 다 받은 뒤 조회해야 서버가 센 메시지를 받은 메시지로 다시 더하지 않는다.
    useEffect(() => {
        if (!ready || roomId === null) return;
        const room = roomId;
        const controller = new AbortController();
        api.call("getChatMessageSummary", {
            params: { path: { roomId: room } },
            signal: controller.signal,
        })
            .then(({ unreadCount: count }) => {
                if (!controller.signal.aborted)
                    setBaseline({ roomId: room, value: { count, until: latest.current } });
            })
            .catch(() => undefined);
        return () => controller.abort();
    }, [ready, roomId]);

    useEffect(() => {
        if (roomId === null) return;
        const room = roomId;
        const current = createReadPositionSender(
            (messageId) =>
                api
                    .call("advanceLastRead", {
                        params: { path: { roomId: room } },
                        body: { lastReadMessageId: messageId },
                    })
                    .then(() => undefined),
            { idleMs: READ_IDLE_MS, maxWaitMs: READ_MAX_WAIT_MS },
        );
        sender.current = current;
        const flushWhenHidden = () => {
            if (document.visibilityState === "hidden") current.flush();
        };
        document.addEventListener("visibilitychange", flushWhenHidden);
        return () => {
            document.removeEventListener("visibilitychange", flushWhenHidden);
            current.flush();
            current.dispose();
            if (sender.current === current) sender.current = null;
        };
    }, [roomId]);

    useEffect(() => {
        if (reading && ready && latestId > 0) sender.current?.mark(latestId);
    }, [reading, ready, latestId]);

    useEffect(() => {
        if (!reading) sender.current?.flush();
    }, [reading]);

    if (reading || roomId === null) return 0;
    return unreadCount(
        messages,
        myUserId,
        baseline?.roomId === roomId ? baseline.value : null,
        readUntil?.roomId === roomId ? readUntil.value : null,
    );
}
