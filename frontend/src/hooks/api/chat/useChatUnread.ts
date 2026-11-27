import { useEffect, useRef, useState } from "react";
import { api } from "@/api/api";
import type { ChatMessage, TripChatStatus } from "../../chat.types";
import { createReadPositionSender, type ReadPositionSender } from "./readPositionSender";
import { createSummaryLoader, type SummaryLoader } from "./summaryLoader";
import { latestMessageId, unreadCount, type UnreadBaseline } from "./unreadCount";

const READ_IDLE_MS = 1000;
const READ_MAX_WAIT_MS = 5000;

/** 다른 방의 값이 남지 않도록 방 ID와 함께 둔다. */
type ForRoom<T> = { roomId: number; value: T };

type ChatUnreadOptions = {
    roomId: number | null;
    status: TripChatStatus;
    messages: ChatMessage[];
    /** useTripChat의 syncedUntil. 이 위치까지만 읽은 것으로 기록한다. */
    syncedUntil: number | null;
    myUserId: number;
    /** 패널을 열어 메시지를 보는 중인지 */
    reading: boolean;
};

/**
 * 채팅방의 안 읽은 메시지 개수.
 * 놓친 메시지를 다 받은 뒤 서버 개수를 받고, 서버가 센 마지막 메시지 이후 도착한 다른 회원의 메시지를 더한다.
 * 서버 개수를 받지 못했으면 새 메시지가 오거나 패널을 열 때 다시 조회한다.
 * 패널을 열어 보는 동안은 0이고, 빈틈없이 받은 마지막 메시지까지 모아서 서버에 읽은 위치로 기록한다.
 * 패널을 닫거나 화면을 벗어나거나 탭이 가려지면 기다리지 않고 보낸다.
 */
export function useChatUnread({
    roomId,
    status,
    messages,
    syncedUntil,
    myUserId,
    reading,
}: ChatUnreadOptions): number {
    const latestId = latestMessageId(messages);
    const readableId = syncedUntil === null ? latestId : Math.min(syncedUntil, latestId);
    const ready = roomId !== null && status === "ready";
    const [baseline, setBaseline] = useState<ForRoom<UnreadBaseline> | null>(null);
    const [readUntil, setReadUntil] = useState<ForRoom<number> | null>(null);
    const [wasReading, setWasReading] = useState(reading);
    const summary = useRef<SummaryLoader | null>(null);
    const sender = useRef<ReadPositionSender | null>(null);

    // 패널을 닫으면 그때까지 빈틈없이 본 메시지 다음부터 센다.
    if (wasReading !== reading) {
        setWasReading(reading);
        if (!reading && roomId !== null) setReadUntil({ roomId, value: readableId });
    }

    useEffect(() => {
        if (!ready || roomId === null) return;
        const room = roomId;
        const loader = createSummaryLoader(
            (signal) =>
                api.call("getChatMessageSummary", { params: { path: { roomId: room } }, signal }),
            ({ unreadCount: count, lastMessageId }) =>
                setBaseline({ roomId: room, value: { count, until: lastMessageId } }),
        );
        summary.current = loader;
        loader.request();
        return () => {
            loader.dispose();
            if (summary.current === loader) summary.current = null;
        };
    }, [ready, roomId]);

    useEffect(() => {
        summary.current?.request();
    }, [latestId, reading]);

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
        if (reading && ready && readableId > 0) sender.current?.mark(readableId);
    }, [reading, ready, readableId]);

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
