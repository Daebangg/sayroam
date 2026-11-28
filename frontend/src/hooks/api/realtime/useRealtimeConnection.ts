import { useEffect } from "react";
import { listenChatQueues } from "@/api/chat/chatSocket";
import { startRealtime, stopRealtime } from "@/api/realtime/realtimeSocket";

/**
 * 로그인한 회원이 있는 동안 탭의 실시간 연결 하나를 유지하고, 연결 직후 개인 큐를 구독한다.
 * 방 채널은 화면이 이 연결 위에서 따로 구독한다. 회원이 바뀌면 이전 회원의 구독이 남지 않도록 연결을 새로 연다.
 */
export function useRealtimeConnection(userId: number | null): void {
    useEffect(() => {
        if (userId === null) return;
        listenChatQueues();
        startRealtime();
        return stopRealtime;
    }, [userId]);
}
