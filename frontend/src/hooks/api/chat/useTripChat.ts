import { useEffect, useRef, useState } from "react";
import { api } from "@/api/api";
import { openChatSocket, type ChatSocket } from "@/api/chat/chatSocket";
import type {
    ChatConnection,
    ChatMessage,
    OutgoingMessage,
    RoomMember,
    TripChatStatus,
} from "../../chat.types";

type TripChat = {
    status: TripChatStatus;
    connection: ChatConnection;
    /** 서버에 저장된 메시지. messageId 오름차순 */
    messages: ChatMessage[];
    /** 아직 서버 확인을 받지 못한 내 메시지. 보낸 순서 */
    outgoing: OutgoingMessage[];
    members: RoomMember[];
    /** 다시 연결된 뒤 명단이나 놓친 메시지를 받지 못해 빠진 내용이 있을 수 있다. */
    syncFailed: boolean;
    send: (content: string) => void;
    retry: (clientMessageId: string) => void;
    discard: (clientMessageId: string) => void;
    retrySync: () => void;
    reload: () => void;
};

const PAGE_SIZE = 100;
// 연결은 살아 있는데 확인이 오지 않는 드문 경우를 위한 안전장치. 거절과 연결 끊김은 바로 실패로 바꾼다.
const SEND_TIMEOUT_MS = 30000;

type Outbox = ReturnType<typeof createOutbox>;

/** 서버 확인 전인 내 메시지와 그 시간 제한을 관리한다. 화면 상태는 publish로 내보낸다. */
function createOutbox(publish: (messages: OutgoingMessage[]) => void) {
    let messages: OutgoingMessage[] = [];
    const timers = new Map<string, number>();

    function update(next: OutgoingMessage[]) {
        messages = next;
        publish(messages);
    }

    function clearTimer(clientMessageId: string) {
        window.clearTimeout(timers.get(clientMessageId));
        timers.delete(clientMessageId);
    }

    function fail(clientMessageId: string) {
        clearTimer(clientMessageId);
        update(
            messages.map((message) =>
                message.clientMessageId === clientMessageId && message.status === "sending"
                    ? { ...message, status: "failed" }
                    : message,
            ),
        );
    }

    return {
        add(clientMessageId: string, content: string) {
            update([...messages, { clientMessageId, content, status: "sending" }]);
        },
        /** 보낸 뒤 확인을 기다린다. 시간 안에 확인이 없으면 실패로 바꾼다. */
        wait(clientMessageId: string) {
            clearTimer(clientMessageId);
            timers.set(
                clientMessageId,
                window.setTimeout(() => fail(clientMessageId), SEND_TIMEOUT_MS),
            );
        },
        fail,
        failAll() {
            timers.forEach((timer) => window.clearTimeout(timer));
            timers.clear();
            if (messages.some((message) => message.status === "sending"))
                update(
                    messages.map((message) =>
                        message.status === "sending" ? { ...message, status: "failed" } : message,
                    ),
                );
        },
        /** 다시 보낼 내용. 실패한 메시지가 아니면 null */
        resend(clientMessageId: string): string | null {
            const message = messages.find((item) => item.clientMessageId === clientMessageId);
            if (!message || message.status !== "failed") return null;
            update(
                messages.map((item) => (item === message ? { ...item, status: "sending" } : item)),
            );
            return message.content;
        },
        remove(clientMessageIds: ReadonlySet<string>) {
            clientMessageIds.forEach(clearTimer);
            if (messages.some((message) => clientMessageIds.has(message.clientMessageId)))
                update(
                    messages.filter((message) => !clientMessageIds.has(message.clientMessageId)),
                );
        },
    };
}

function mergeMessages(current: ChatMessage[], incoming: ChatMessage[]): ChatMessage[] {
    const known = new Set(current.map((message) => message.messageId));
    const added = incoming.filter((message) => !known.has(message.messageId));
    if (added.length === 0) return current;
    return [...current, ...added].sort((a, b) => a.messageId - b.messageId);
}

/** 한 번에 하나만 돌린다. 도는 중에 다시 요청되면 끝난 뒤 한 번 더 돌린다. */
function runOneAtATime(task: () => Promise<void>, signal: AbortSignal): () => Promise<void> {
    let running: Promise<void> | null = null;
    let again = false;
    return () => {
        if (running) {
            again = true;
            return running;
        }
        running = (async () => {
            try {
                do {
                    again = false;
                    await task();
                } while (again && !signal.aborted);
            } finally {
                running = null;
            }
        })();
        return running;
    };
}

/**
 * 여행 상세에 있는 동안 혼자여도 채팅방에 연결해 둔다.
 * 연결될 때마다 명단과 놓친 메시지를 받고, 연결된 동안의 멤버 변화는 서버 신호를 받아 명단을 다시 받는다.
 * 패널을 닫아도 받은 메시지와 연결을 유지해 다시 열면 바로 보여 준다.
 */
export function useTripChat(roomId: number | null, myUserId: number): TripChat {
    const [status, setStatus] = useState<TripChatStatus>("loading");
    const [connection, setConnection] = useState<ChatConnection>("connecting");
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [outgoing, setOutgoing] = useState<OutgoingMessage[]>([]);
    const [members, setMembers] = useState<RoomMember[]>([]);
    const [syncFailed, setSyncFailed] = useState(false);
    const [outbox] = useState<Outbox>(() => createOutbox(setOutgoing));
    const socket = useRef<ChatSocket | null>(null);
    const loadAll = useRef<() => void>(() => undefined);
    const resync = useRef<() => void>(() => undefined);

    useEffect(() => {
        if (roomId === null) return;
        const room = roomId;
        const controller = new AbortController();
        const { signal } = controller;
        // 놓친 메시지 조회 위치. 실시간 메시지는 앞 구간이 아직 조회되지 않았을 수 있어 조회 결과로만 옮긴다.
        let fetchedUntil = 0;
        let connectedBefore = false;

        function receive(incoming: ChatMessage[]) {
            if (incoming.length === 0) return;
            setMessages((current) => mergeMessages(current, incoming));
            const confirmed = new Set<string>();
            incoming.forEach((message) => {
                if (message.senderId === myUserId && message.clientMessageId)
                    confirmed.add(message.clientMessageId);
            });
            outbox.remove(confirmed);
        }

        // 신호를 받기 전에 시작한 조회는 바뀌기 전 명단을 읽었을 수 있어, 도는 중에 온 신호는 끝난 뒤 한 번 더 받는다.
        const loadMembers = runOneAtATime(async () => {
            const { members: list } = await api.call("findMembers", {
                params: { path: { roomId: room } },
                signal,
            });
            if (!signal.aborted) setMembers(list);
        }, signal);

        async function loadMessagesAfter(): Promise<void> {
            for (;;) {
                const page = await api.call("findAfter", {
                    params: {
                        path: { roomId: room },
                        query: { afterId: fetchedUntil, size: PAGE_SIZE },
                    },
                    signal,
                });
                if (signal.aborted) return;
                receive(page);
                fetchedUntil = page.at(-1)?.messageId ?? fetchedUntil;
                if (page.length < PAGE_SIZE) return;
            }
        }

        const catchUp = runOneAtATime(loadMessagesAfter, signal);

        loadAll.current = () => {
            Promise.all([loadMembers(), catchUp()])
                .then(() => {
                    if (signal.aborted) return;
                    setStatus("ready");
                    setSyncFailed(false);
                })
                .catch(() => {
                    if (!signal.aborted) setStatus("failed");
                });
        };

        // 다시 연결된 뒤 명단이나 놓친 메시지를 받지 못하면 받은 것은 그대로 두고 빠진 내용이 있다고 알린다.
        resync.current = () => {
            Promise.all([loadMembers(), catchUp()])
                .then(() => {
                    if (!signal.aborted) setSyncFailed(false);
                })
                .catch(() => {
                    if (!signal.aborted) setSyncFailed(true);
                });
        };

        // 구독을 마친 뒤 조회해야 그 사이에 온 메시지와 멤버 변화가 빠지지 않는다. 중복 메시지는 messageId로 거른다.
        socket.current = openChatSocket(room, {
            onConnected: () => {
                setConnection("connected");
                if (connectedBefore) resync.current();
                else loadAll.current();
                connectedBefore = true;
            },
            onDisconnected: () => {
                setConnection("reconnecting");
                outbox.failAll();
            },
            onMessage: (message) => receive([message]),
            onMembersChanged: () => void loadMembers().catch(() => undefined),
            onSendError: (error) => outbox.fail(error.clientMessageId),
            onClosed: () => {
                setConnection("closed");
                outbox.failAll();
                // 한 번도 연결되지 못하고 멈췄으면 불러오는 중으로 남지 않게 한다.
                if (!connectedBefore) setStatus("failed");
            },
        });

        return () => {
            controller.abort();
            socket.current?.close();
            socket.current = null;
            outbox.failAll();
        };
    }, [roomId, myUserId, outbox]);

    function dispatch(clientMessageId: string, content: string) {
        if (socket.current?.send(clientMessageId, content)) outbox.wait(clientMessageId);
        else outbox.fail(clientMessageId);
    }

    function send(content: string) {
        const clientMessageId = crypto.randomUUID();
        outbox.add(clientMessageId, content);
        dispatch(clientMessageId, content);
    }

    // 같은 clientMessageId로 다시 보내므로 앞선 전송이 실제로 저장됐어도 서버가 한 번만 저장한다.
    function retry(clientMessageId: string) {
        const content = outbox.resend(clientMessageId);
        if (content !== null) dispatch(clientMessageId, content);
    }

    function discard(clientMessageId: string) {
        outbox.remove(new Set([clientMessageId]));
    }

    function retrySync() {
        resync.current();
    }

    function reload() {
        setStatus("loading");
        loadAll.current();
    }

    if (roomId === null)
        return {
            status: "idle",
            connection: "closed",
            messages: [],
            outgoing: [],
            members: [],
            syncFailed: false,
            send,
            retry,
            discard,
            retrySync,
            reload,
        };
    return {
        status,
        connection,
        messages,
        outgoing,
        members,
        syncFailed,
        send,
        retry,
        discard,
        retrySync,
        reload,
    };
}
