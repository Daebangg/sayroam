import { Client, type IFrame, type IMessage, type StompSubscription } from "@stomp/stompjs";
import { api } from "../api.ts";
import { refreshAccessToken } from "../auth/refreshSession.ts";
import { getAccessTokenSnapshot } from "../auth/tokenStore.ts";

/** 연결 상태를 받는 쪽. 기능별 채널(채팅 등)이 연결 위에서 구독을 관리할 때 쓴다. */
export type RealtimeListener = {
    /** 연결될 때마다 불린다. 끊긴 뒤 다시 연결되면 구독이 모두 사라졌으므로 여기서 다시 구독한다. */
    onConnected: () => void;
    onDisconnected: () => void;
    /** 서버가 ERROR 프레임으로 연결을 끊었다. receiptId는 거절된 프레임에 receipt가 있었을 때만 있다. */
    onRejected: (code: string, receiptId: string | undefined) => void;
    /** 로그인이 끝나 더 연결하지 않는다. */
    onStopped: () => void;
};

const RECONNECT_DELAY_MS = 3000;
const HEARTBEAT_MS = 10000;
// 연결 중 만료되지 않도록 남은 시간이 이보다 짧으면 연결 전에 갱신한다.
const TOKEN_REFRESH_MARGIN_MS = 30000;
// 권한 거절은 토큰 문제가 아니므로 다음 연결 전에 토큰을 갱신하지 않는다.
const ACCESS_DENIED_CODES = new Set(["ACCESS_DENIED", "ROOM_ACCESS_DENIED"]);

const listeners = new Set<RealtimeListener>();
let client: Client | null = null;
// 서버가 연결을 거절했으면 만료 전이라도 토큰이 무효화됐을 수 있어 다음 연결 전에 새로 받는다.
let refreshBeforeConnect = false;

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

/** 탭마다 연결 하나를 연다. 이미 열려 있으면 아무것도 하지 않는다. 로그인한 동안 유지한다. */
export function startRealtime(): void {
    if (client) return;
    const next: Client = new Client({
        brokerURL: socketUrl(),
        reconnectDelay: RECONNECT_DELAY_MS,
        heartbeatIncoming: HEARTBEAT_MS,
        heartbeatOutgoing: HEARTBEAT_MS,
        beforeConnect: async () => {
            try {
                const token = await accessToken(refreshBeforeConnect);
                refreshBeforeConnect = false;
                next.connectHeaders = { Authorization: `Bearer ${token}` };
            } catch {
                // 갱신까지 실패하면 로그인이 풀린 것이다. 공통 기반이 로그아웃을 처리한다.
                stopRealtime();
            }
        },
        onConnect: () => listeners.forEach((listener) => listener.onConnected()),
        onStompError: (frame: IFrame) => {
            const code = frame.headers.message ?? "";
            if (!ACCESS_DENIED_CODES.has(code)) refreshBeforeConnect = true;
            listeners.forEach((listener) => listener.onRejected(code, frame.headers["receipt-id"]));
        },
        onWebSocketClose: () => {
            if (client === next) listeners.forEach((listener) => listener.onDisconnected());
        },
    });
    client = next;
    next.activate();
}

export function stopRealtime(): void {
    const current = client;
    if (!current) return;
    client = null;
    refreshBeforeConnect = false;
    void current.deactivate();
    listeners.forEach((listener) => listener.onStopped());
}

export function addRealtimeListener(listener: RealtimeListener): () => void {
    listeners.add(listener);
    return () => {
        listeners.delete(listener);
    };
}

export function isRealtimeConnected(): boolean {
    return client?.connected ?? false;
}

/**
 * 연결된 동안에만 구독할 수 있다. 연결되지 않았으면 null이고, 다음 onConnected에서 다시 구독한다.
 * receipt를 주면 서버가 구독 등록을 마친 뒤 onReceived가 불린다.
 */
export function subscribeRealtime(
    destination: string,
    onMessage: (frame: IMessage) => void,
    receipt?: { id: string; onReceived: () => void },
): StompSubscription | null {
    if (!client?.connected) return null;
    if (!receipt) return client.subscribe(destination, onMessage);
    client.watchForReceipt(receipt.id, receipt.onReceived);
    return client.subscribe(destination, onMessage, { receipt: receipt.id });
}

/** 연결되어 있지 않으면 보내지 않고 false를 돌려준다. */
export function publishRealtime(destination: string, body: string): boolean {
    if (!client?.connected) return false;
    client.publish({ destination, body });
    return true;
}
