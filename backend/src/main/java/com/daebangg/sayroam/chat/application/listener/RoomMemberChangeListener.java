package com.daebangg.sayroam.chat.application.listener;

import com.daebangg.sayroam.chat.application.port.out.RoomMemberChangeBroadcaster;
import com.daebangg.sayroam.room.application.service.RoomService;
import com.daebangg.sayroam.room.domain.event.RoomMemberJoined;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import com.daebangg.sayroam.user.domain.event.UserProfileChanged;
import com.daebangg.sayroam.user.domain.event.UserWithdrawn;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 리스너 예외는 이미 커밋된 요청의 응답을 실패로 바꾸므로 조회·발송 실패는 로그만 남긴다.
@Slf4j
@Component
@RequiredArgsConstructor
public class RoomMemberChangeListener {
    private final RoomService roomService;
    private final RoomMemberChangeBroadcaster broadcaster;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onJoined(RoomMemberJoined event) {
        broadcast(event.roomId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLeft(RoomMemberLeft event) {
        broadcast(event.roomId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfileChanged(UserProfileChanged event) {
        broadcastToRoomsOf(event.userId());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWithdrawn(UserWithdrawn event) {
        broadcastToRoomsOf(event.userId());
    }
    
    // 커밋된 트랜잭션에 참여해 조회만 한다. 쓰기가 필요해지면 REQUIRES_NEW로 분리한다.
    private void broadcastToRoomsOf(long userId) {
        try {
            roomService.findRoomIds(userId).forEach(this::broadcast);
        } catch (RuntimeException exception) {
            log.warn("Failed to find rooms for member change: userId={}", userId, exception);
        }
    }

    private void broadcast(Long roomId) {
        try {
            broadcaster.broadcast(roomId);
        } catch (RuntimeException exception) {
            log.warn("Failed to broadcast member change: roomId={}", roomId, exception);
        }
    }
}
