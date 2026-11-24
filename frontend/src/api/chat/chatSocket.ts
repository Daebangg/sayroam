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
    /** 연결과 구독이 끝났다. 다시 연결될 때마다 불리므로 그 사이 놓친 메시지를 여기서 이어 받는다. */
    onConnected: () => void;
    onDisconnected: () => void;
    /** 방 채널과 내 전송 확인 채널로 받은 메시지. 같은 메시지가 두 채널로 올 수 있다. */
    onMessage: (message: ChatMessage) => void;
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
            const receive = (frame: IMessage) => {
                const message = parseChatMessage(frame.body);
                if (message) handlers.onMessage(message);
            };
            client.subscribe(`/topic/rooms/${roomId}`, receive);
            client.subscribe("/user/queue/messages", receive);
            client.subscribe("/user/queue/errors", (frame) => {
                const error = parseChatSendError(frame.body);
                if (error) handlers.onSendError(error);
            });
            handlers.onConnected();
        },
        onStompError: (frame: IFrame) => {
            // 방을 나갔거나 내보내져 구독이 거절되면 다시 연결해도 같은 결과라 멈춘다.
            if (frame.headers.message === "ACCESS_DENIED") stop();
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
