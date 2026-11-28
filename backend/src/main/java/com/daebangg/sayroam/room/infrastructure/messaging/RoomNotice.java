package com.daebangg.sayroam.room.infrastructure.messaging;

// 개인 큐 /user/queue/rooms로 보내는 방 멤버십 알림. 클라이언트는 type으로 처리를 나눈다.
public record RoomNotice(Type type, Long roomId) {
    public enum Type {
        // 방에서 빠졌다. 받은 클라이언트는 그 방 채널 구독을 해제한다.
        LEFT
    }

    public static RoomNotice left(Long roomId) {
        return new RoomNotice(Type.LEFT, roomId);
    }
}
