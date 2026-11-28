import type { IMessage, StompSubscription } from "@stomp/stompjs";
import {
    addRealtimeListener,
    isRealtimeConnected,
    publishRealtime,
    subscribeRealtime,
} from "../realtime/realtimeSocket.ts";
import {
    parseChatMessage,
    parseChatSendError,
    parseRoomNotice,
    type ChatMessage,
    type ChatSendError,
} from "./chatResponse.ts";

type ChatSocketHandlers = {
    /** 연결되고 서버가 방 채널 구독 등록을 확인했다. 다시 연결될 때마다 불리므로 그 사이 놓친 내용을 여기서 이어 받는다. */
    onConnected: () => void;
    onDisconnected: () => void;
    /** 방 채널과 내 전송 확인 채널로 받은 메시지. 같은 메시지가 두 채널로 올 수 있다. */
    onMessage: (message: ChatMessage) => void;
    /** 참여·나감·프로필 변경·탈퇴로 멤버 명단이 바뀌었다. 내용은 없으므로 명단을 다시 받는다. */
    onMembersChanged: () => void;
    onSendError: (error: ChatSendError) => void;
    /** 방에 참여 중이 아니거나 로그인이 풀려 더 받지 않는다. */
    onClosed: () => void;
};

export type ChatSocket = {
    /** 연결되어 있지 않으면 보내지 않고 false를 돌려준다. */
    send: (clientMessageId: string, content: string) => boolean;
    close: () => void;
};

/** 방 하나의 구독. 연결이 끊기면 구독이 사라지므로 다시 연결될 때마다 새로 구독한다. */
type RoomEntry = {
    roomId: number;
    handlers: ChatSocketHandlers;
    subscriptions: StompSubscription[];
    /** 이번 연결에서 아직 등록 확인을 받지 못한 구독의 receipt */
    pendingReceipts: Set<string>;
};

const entries = new Set<RoomEntry>();
let listening = false;

function roomTopic(roomId: number): string {
    return `/topic/rooms/${roomId}`;
}

/**
 * 연결 하나를 여러 방이 함께 쓴다. 개인 큐는 연결마다 한 번 구독하고 받은 내용을 방별로 나눠 준다.
 * 연결을 시작하기 전에 등록해 연결 직후 개인 큐를 구독한다. 이미 연결돼 있으면 바로 구독한다.
 */
export function listenChatQueues(): void {
    if (listening) return;
    listening = true;
    if (isRealtimeConnected()) subscribePersonalQueues();
    addRealtimeListener({
        onConnected: () => {
            subscribePersonalQueues();
            entries.forEach(subscribeRoom);
        },
        onDisconnected: () =>
            entries.forEach((entry) => {
                entry.subscriptions = [];
                entry.pendingReceipts.clear();
                entry.handlers.onDisconnected();
            }),
        onRejected: (code, receiptId) => {
            // 허용되지 않은 경로는 클라이언트 오류라 다시 연결해도 같으므로 모든 방을 멈춘다.
            if (code === "ACCESS_DENIED") [...entries].forEach(closeRoom);
            // 방 구독이 거절되면 그 방만 멈추고 나머지 방은 다시 연결해 이어 받는다.
            // 서버가 거절된 구독의 receipt를 돌려주지 않으면 아직 확인받지 못한 방을 멈춘다.
            if (code === "ROOM_ACCESS_DENIED")
                [...entries]
                    .filter((entry) =>
                        receiptId
                            ? entry.pendingReceipts.has(receiptId)
                            : entry.pendingReceipts.size > 0,
                    )
                    .forEach(closeRoom);
        },
        onStopped: () => [...entries].forEach(closeRoom),
    });
}

function subscribePersonalQueues(): void {
    // 전송 확인은 같은 계정의 모든 연결로 오므로 다른 탭에서 다른 방에 보낸 메시지도 온다.
    subscribeRealtime("/user/queue/messages", (frame: IMessage) => {
        const message = parseChatMessage(frame.body);
        if (message) forRoom(message.roomId, (entry) => entry.handlers.onMessage(message));
    });
    // 오류에는 방 ID가 없다. clientMessageId가 맞는 방만 실패로 바꾼다.
    subscribeRealtime("/user/queue/errors", (frame: IMessage) => {
        const error = parseChatSendError(frame.body);
        if (error) entries.forEach((entry) => entry.handlers.onSendError(error));
    });
    // 다른 탭·기기에서 나갔거나 탈퇴로 빠진 방은 서버가 알려 준다. 구독을 남겨 두면 서버가 연결을 끊는다.
    subscribeRealtime("/user/queue/rooms", (frame: IMessage) => {
        const notice = parseRoomNotice(frame.body);
        if (notice?.type === "LEFT") forRoom(notice.roomId, closeRoom);
    });
}

function forRoom(roomId: number, action: (entry: RoomEntry) => void): void {
    [...entries].filter((entry) => entry.roomId === roomId).forEach(action);
}

// 방 채널은 서버가 등록을 마쳤다고 RECEIPT로 알려 준 뒤에 조회해야 그 사이 변화를 놓치지 않는다.
function subscribeRoom(entry: RoomEntry): void {
    const subscribeConfirmed = (destination: string, onMessage: (frame: IMessage) => void) => {
        const receipt = crypto.randomUUID();
        const subscription = subscribeRealtime(destination, onMessage, {
            id: receipt,
            onReceived: () => {
                if (!entries.has(entry) || !entry.pendingReceipts.delete(receipt)) return;
                if (entry.pendingReceipts.size === 0) entry.handlers.onConnected();
            },
        });
        if (!subscription) return;
        entry.pendingReceipts.add(receipt);
        entry.subscriptions.push(subscription);
    };
    subscribeConfirmed(roomTopic(entry.roomId), (frame) => {
        const message = parseChatMessage(frame.body);
        if (message?.roomId === entry.roomId) entry.handlers.onMessage(message);
    });
    subscribeConfirmed(`${roomTopic(entry.roomId)}/members`, () =>
        entry.handlers.onMembersChanged(),
    );
}

function unsubscribeRoom(entry: RoomEntry): void {
    if (isRealtimeConnected())
        entry.subscriptions.forEach((subscription) => subscription.unsubscribe());
    entry.subscriptions = [];
    entry.pendingReceipts.clear();
}

function closeRoom(entry: RoomEntry): void {
    if (!entries.delete(entry)) return;
    unsubscribeRoom(entry);
    entry.handlers.onClosed();
}

/** 앱 공용 연결 위에서 방 채널을 구독한다. 닫으면 연결은 두고 이 방의 구독만 해제한다. */
export function openChatSocket(roomId: number, handlers: ChatSocketHandlers): ChatSocket {
    listenChatQueues();
    const entry: RoomEntry = { roomId, handlers, subscriptions: [], pendingReceipts: new Set() };
    entries.add(entry);
    if (isRealtimeConnected()) subscribeRoom(entry);

    return {
        send(clientMessageId, content) {
            if (!entries.has(entry)) return false;
            return publishRealtime(
                `/app/rooms/${roomId}/messages`,
                JSON.stringify({ clientMessageId, content }),
            );
        },
        close() {
            if (!entries.delete(entry)) return;
            unsubscribeRoom(entry);
        },
    };
}
