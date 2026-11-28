import { ApiError } from "../apiError.ts";
import type { components } from "../generated/schema.ts";

export type ChatMessage = components["schemas"]["ChatMessageResponse"];
export type RoomMember = components["schemas"]["RoomMemberProfileResponse"];
/** 전송을 거절당한 메시지. 서버가 개인 오류 채널로 보낸다. */
export type ChatSendError = { clientMessageId: string; code: string };

const memberStatuses = new Set(["JOINED", "LEFT", "KICKED"]);

function record(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function id(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= 1;
}

function text(value: unknown): value is string {
    return typeof value === "string";
}

function chatMessage(value: unknown): value is ChatMessage {
    return (
        record(value) &&
        id(value.messageId) &&
        id(value.roomId) &&
        (value.senderId === null || id(value.senderId)) &&
        (value.clientMessageId === null || text(value.clientMessageId)) &&
        text(value.content) &&
        text(value.createdAt) &&
        Number.isFinite(Date.parse(value.createdAt))
    );
}

function roomMember(value: unknown): value is RoomMember {
    return (
        record(value) &&
        id(value.userId) &&
        text(value.nickname) &&
        (value.profileImageUrl === null || text(value.profileImageUrl)) &&
        text(value.status) &&
        memberStatuses.has(value.status)
    );
}

export function validateChatMessages(value: unknown, status: number): ChatMessage[] {
    if (!Array.isArray(value) || !value.every(chatMessage))
        throw new ApiError("invalid-response", { status });
    return value;
}

export function validateChatMessageSummary(
    value: unknown,
    status: number,
): components["schemas"]["ChatMessageSummaryResponse"] {
    if (!record(value) || !count(value.unreadCount) || !count(value.lastMessageId))
        throw new ApiError("invalid-response", { status });
    return { unreadCount: value.unreadCount, lastMessageId: value.lastMessageId };
}

function count(value: unknown): value is number {
    return typeof value === "number" && Number.isSafeInteger(value) && value >= 0;
}

export function validateRoomMembers(
    value: unknown,
    status: number,
): components["schemas"]["RoomMemberListResponse"] {
    if (!record(value) || !Array.isArray(value.members) || !value.members.every(roomMember))
        throw new ApiError("invalid-response", { status });
    return { members: value.members };
}

/** 실시간 채널로 받은 메시지. 형식이 맞지 않으면 버리고 다음 이어 받기에서 다시 받는다. */
export function parseChatMessage(body: string): ChatMessage | null {
    const value = parseJson(body);
    return chatMessage(value) ? value : null;
}

export function parseChatSendError(body: string): ChatSendError | null {
    const value = parseJson(body);
    return record(value) && text(value.clientMessageId) && text(value.code)
        ? { clientMessageId: value.clientMessageId, code: value.code }
        : null;
}

/** 개인 방 알림. 모르는 type은 새로 추가된 알림이므로 버린다. */
export type RoomNotice = { type: "LEFT"; roomId: number };

export function parseRoomNotice(body: string): RoomNotice | null {
    const value = parseJson(body);
    return record(value) && value.type === "LEFT" && id(value.roomId)
        ? { type: "LEFT", roomId: value.roomId }
        : null;
}

function parseJson(body: string): unknown {
    try {
        return JSON.parse(body);
    } catch {
        return null;
    }
}
