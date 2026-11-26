import { Client, type IFrame, type IMessage } from "@stomp/stompjs";
import { api } from "../api.ts";
import { refreshAccessToken } from "../auth/refreshSession.ts";
import { getAccessTokenSnapshot } from "../auth/tokenStore.ts";
import {
    parseChatMessage,
    parseChatSendError,
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
    /** 방에 참여 중이 아니거나 로그인이 풀려 더 연결하지 않는다. */
    onClosed: () => void;
};

export type ChatSocket = {
    /** 연결되어 있지 않으면 보내지 않고 false를 돌려준다. */
    send: (clientMessageId: string, content: string) => boolean;
    close: () => void;
};

const RECONNECT_DELAY_MS = 3000;
const HEARTBEAT_MS = 10000;
// 연결 중 만료되지 않도록 남은 시간이 이보다 짧으면 연결 전에 갱신한다.
const TOKEN_REFRESH_MARGIN_MS = 30000;
// 허용되지 않은 구독은 ACCESS_DENIED, 방 멤버가 아니거나 방을 나간 경우는 ROOM_ACCESS_DENIED로 온다.
const ACCESS_DENIED_CODES = new Set(["ACCESS_DENIED", "ROOM_ACCESS_DENIED"]);

function socketUrl(): string {
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    return `${protocol}//${window.location.host}/ws`;
}

async function accessToken(forceRefresh: boolean): Promise<string> {
    const snapshot = getAccessTokenSnapshot();
    if (!forceRefresh && snapshot && snapshot.expiresAt - Date.now() > TOKEN_REFRESH_MARGIN_MS)
        return snapshot.value;
    return (await refreshAccessToken(() => api.call("refresh"))).value;
}

export function openChatSocket(roomId: number, handlers: ChatSocketHandlers): ChatSocket {
    let closed = false;
    // 서버가 연결을 거절했으면 만료 전이라도 토큰이 무효화됐을 수 있어 다음 연결 전에 새로 받는다.
    let refreshBeforeConnect = false;
    const client = new Client({
        brokerURL: socketUrl(),
        reconnectDelay: RECONNECT_DELAY_MS,
        heartbeatIncoming: HEARTBEAT_MS,
        heartbeatOutgoing: HEARTBEAT_MS,
        beforeConnect: async () => {
            try {
                const token = await accessToken(refreshBeforeConnect);
                refreshBeforeConnect = false;
                client.connectHeaders = { Authorization: `Bearer ${token}` };
            } catch {
                // 갱신까지 실패하면 로그인이 풀린 것이다. 공통 기반이 로그아웃을 처리한다.
                stop();
            }
        },
        onConnect: () => {
            // 전송 확인 채널은 같은 계정의 모든 연결로 가므로, 다른 탭에서 다른 방에 보낸 메시지도 온다.
            const receive = (frame: IMessage) => {
                const message = parseChatMessage(frame.body);
                if (message?.roomId === roomId) handlers.onMessage(message);
            };
            client.subscribe("/user/queue/messages", receive);
            client.subscribe("/user/queue/errors", (frame) => {
                const error = parseChatSendError(frame.body);
                if (error) handlers.onSendError(error);
            });
            // 방 채널은 서버가 등록을 마쳤다고 RECEIPT로 알려 준 뒤에 조회해야 그 사이 변화를 놓치지 않는다.
            // 개인 큐는 내가 보낸 메시지의 응답만 오고 방 채널과 조회로도 맞춰지므로 기다리지 않는다.
            let pending = 2;
            const subscribeConfirmed = (
                destination: string,
                callback: (frame: IMessage) => void,
            ) => {
                const receipt = crypto.randomUUID();
                client.watchForReceipt(receipt, () => {
                    pending -= 1;
                    if (pending === 0) handlers.onConnected();
                });
                client.subscribe(destination, callback, { receipt });
            };
            subscribeConfirmed(`/topic/rooms/${roomId}`, receive);
            subscribeConfirmed(`/topic/rooms/${roomId}/members`, () => handlers.onMembersChanged());
        },
        onStompError: (frame: IFrame) => {
            // 방을 나갔거나 내보내져 구독이 거절되면 다시 연결해도 같은 결과라 멈춘다.
            if (ACCESS_DENIED_CODES.has(frame.headers.message ?? "")) stop();
            else refreshBeforeConnect = true;
        },
        onWebSocketClose: () => {
            if (!closed) handlers.onDisconnected();
        },
    });

    function stop() {
        if (closed) return;
        closed = true;
        void client.deactivate();
        handlers.onClosed();
    }

    client.activate();

    return {
        send(clientMessageId, content) {
            if (!client.connected) return false;
            client.publish({
                destination: `/app/rooms/${roomId}/messages`,
                body: JSON.stringify({ clientMessageId, content }),
            });
            return true;
        },
        close() {
            closed = true;
            void client.deactivate();
        },
    };
}
