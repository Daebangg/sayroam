import type { components } from "@/api/generated/schema";

export type ChatMessage = components["schemas"]["ChatMessageResponse"];
export type RoomMember = components["schemas"]["RoomMemberProfileResponse"];
export type TripChatStatus = "idle" | "loading" | "ready" | "failed";
/** connecting은 처음 연결 중, reconnecting은 끊겨서 다시 연결 중, closed는 더 연결하지 않는 상태 */
export type ChatConnection = "connecting" | "connected" | "reconnecting" | "closed";
/** 아직 서버 확인을 받지 못한 내 메시지 */
export type OutgoingMessage = {
    clientMessageId: string;
    content: string;
    status: "sending" | "failed";
};
