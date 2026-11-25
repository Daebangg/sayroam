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
    /** 다시 연결된 뒤 놓친 메시지를 받지 못해 빠진 구간이 있을 수 있다. */
    syncFailed: boolean;
    send: (content: string) => void;
    retry: (clientMessageId: string) => void;
    discard: (clientMessageId: string) => void;
    retrySync: () => void;
    reload: () => void;
    /** 채팅을 열 때 참여 멤버를 다시 확인한다. 친구가 들어와 있으면 연결을 시작한다. */
    recheck: () => void;
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

/**
 * 여행 상세에 있는 동안 채팅방에 연결해 둔다. 참여 멤버가 나뿐이면 대화할 사람이 없어 연결하지 않는다.
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
    const recheckMembers = useRef<() => void>(() => undefined);

    useEffect(() => {
        if (roomId === null) return;
        const room = roomId;
        const controller = new AbortController();
        const { signal } = controller;
        const memberIds = new Set<number>();
        // 놓친 메시지 조회 위치. 실시간 메시지는 앞 구간이 아직 조회되지 않았을 수 있어 조회 결과로만 옮긴다.
        let fetchedUntil = 0;
        let catchingUp: Promise<void> | null = null;
        let catchUpAgain = false;
        let loadingMembers: Promise<RoomMember[]> | null = null;
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
            // 채팅 중 새로 들어온 멤버가 보낸 메시지면 명단을 다시 받는다.
            if (
                incoming.some(
                    (message) => message.senderId !== null && !memberIds.has(message.senderId),
                )
            )
                void loadMembers().catch(() => undefined);
        }

        function loadMembers(): Promise<RoomMember[]> {
            loadingMembers ??= api
                .call("findMembers", { params: { path: { roomId: room } }, signal })
                .then(({ members: list }) => {
                    if (!signal.aborted) {
                        list.forEach((member) => memberIds.add(member.userId));
                        setMembers(list);
                    }
                    return list;
                })
                .finally(() => {
                    loadingMembers = null;
                });
            return loadingMembers;
        }

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

        // 이어 받기는 하나씩만 돌린다. 도는 중에 다시 요청되면 끝난 뒤 한 번 더 받는다.
        function catchUp(): Promise<void> {
            if (catchingUp) {
                catchUpAgain = true;
                return catchingUp;
            }
            catchingUp = (async () => {
                try {
                    do {
                        catchUpAgain = false;
                        await loadMessagesAfter();
                    } while (catchUpAgain && !signal.aborted);
                } finally {
                    catchingUp = null;
                }
            })();
            return catchingUp;
        }

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

        // 다시 연결된 뒤 놓친 메시지를 받지 못하면 받은 메시지는 그대로 두고 빠진 구간이 있다고 알린다.
        resync.current = () => {
            catchUp()
                .then(() => {
                    if (!signal.aborted) setSyncFailed(false);
                })
                .catch(() => {
                    if (!signal.aborted) setSyncFailed(true);
                });
        };

        // 구독을 마친 뒤 이어 받아야 그 사이에 온 메시지가 빠지지 않는다. 중복은 messageId로 거른다.
        function startChat() {
            if (socket.current || signal.aborted) return;
            socket.current = openChatSocket(room, {
                onConnected: () => {
                    setConnection("connected");
                    resync.current();
                    if (connectedBefore) void loadMembers().catch(() => undefined);
                    connectedBefore = true;
                },
                onDisconnected: () => {
                    setConnection("reconnecting");
                    outbox.failAll();
                },
                onMessage: (message) => receive([message]),
                onSendError: (error) => outbox.fail(error.clientMessageId),
                onClosed: () => {
                    setConnection("closed");
                    outbox.failAll();
                },
            });
            loadAll.current();
        }

        // 친구가 들어와 둘 이상이 되면 그때 연결한다. 혼자면 명단만 받고 끝낸다.
        recheckMembers.current = () => {
            loadMembers()
                .then((list) => {
                    if (signal.aborted) return;
                    if (list.filter((member) => member.status === "JOINED").length > 1) startChat();
                    else setStatus("ready");
                })
                .catch(() => {
                    if (!signal.aborted && !socket.current) setStatus("failed");
                });
        };
        recheckMembers.current();

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
        if (socket.current) loadAll.current();
        else recheckMembers.current();
    }

    // 채팅 패널을 열 때 부른다. 그사이 친구가 참여했으면 연결을 시작한다.
    function recheck() {
        if (!socket.current) recheckMembers.current();
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
            recheck,
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
        recheck,
    };
}
