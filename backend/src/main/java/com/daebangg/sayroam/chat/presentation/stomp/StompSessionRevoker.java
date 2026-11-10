package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.room.domain.RoomErrorCode;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpSession;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.function.Predicate;

// 수신만 하는 연결은 다시 확인할 시점이 없으므로 권한이 사라진 커밋 직후 서버가 직접 연결을 끊는다.
@Slf4j
@Component
public class StompSessionRevoker {
    private final SimpUserRegistry userRegistry;
    private final StompSessionCloser sessionCloser;

    public StompSessionRevoker(SimpUserRegistry userRegistry, StompSessionCloser sessionCloser) {
        this.userRegistry = userRegistry;
        this.sessionCloser = sessionCloser;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoginsInvalidated(LoginsInvalidated event) {
        closeSessions(event.userId(), session -> true, CommonErrorCode.UNAUTHENTICATED);
    }

    // 같은 연결의 다른 방 구독도 함께 끊긴다. 클라이언트는 재연결한 뒤 남은 방을 다시 구독한다.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomMemberLeft(RoomMemberLeft event) {
        String roomTopic = "/topic/rooms/" + event.roomId();
        closeSessions(event.userId(), session -> session.getSubscriptions().stream()
                .anyMatch(subscription -> subscription.getDestination().equals(roomTopic)),
                RoomErrorCode.ROOM_ACCESS_DENIED);
    }

    private void closeSessions(Long userId, Predicate<SimpSession> target, ErrorCode errorCode) {
        SimpUser user = userRegistry.getUser(userId.toString());
        if (user == null) {
            return;
        }
        user.getSessions().stream()
                .filter(target)
                .forEach(session -> close(session.getId(), errorCode));
    }

    // 리스너 예외는 이미 커밋된 요청의 응답을 실패로 바꾼다. 연결은 늦어도 토큰 만료 시각에 끊긴다.
    private void close(String sessionId, ErrorCode errorCode) {
        try {
            sessionCloser.close(sessionId, errorCode);
        } catch (RuntimeException exception) {
            log.warn("Failed to close revoked STOMP session: {}", sessionId, exception);
        }
    }
}
