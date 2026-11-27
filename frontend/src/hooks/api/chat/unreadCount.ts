import type { ChatMessage } from "../../../api/chat/chatResponse.ts";

/** 서버가 센 안 읽은 개수와, 그 개수를 받을 때 이미 받아 둔 마지막 메시지 ID */
export type UnreadBaseline = { count: number; until: number };

/** 메시지는 messageId 오름차순이다. 메시지가 없으면 0 */
export function latestMessageId(messages: readonly ChatMessage[]): number {
    return messages.at(-1)?.messageId ?? 0;
}

function countOthersAfter(
    messages: readonly ChatMessage[],
    myUserId: number,
    after: number,
): number {
    return messages.filter((message) => message.messageId > after && message.senderId !== myUserId)
        .length;
}

/**
 * 이 화면에서 패널을 닫은 위치(readUntil)가 있으면 그 이후만 센다.
 * 없으면 서버 개수에 그 뒤 도착한 다른 회원의 메시지를 더한다. 서버 개수를 받기 전에는 0이다.
 */
export function unreadCount(
    messages: readonly ChatMessage[],
    myUserId: number,
    baseline: UnreadBaseline | null,
    readUntil: number | null,
): number {
    if (readUntil !== null) return countOthersAfter(messages, myUserId, readUntil);
    if (baseline === null) return 0;
    return baseline.count + countOthersAfter(messages, myUserId, baseline.until);
}
