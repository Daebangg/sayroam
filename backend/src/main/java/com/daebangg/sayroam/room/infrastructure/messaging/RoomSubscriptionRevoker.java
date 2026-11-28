package com.daebangg.sayroam.room.infrastructure.messaging;

import com.daebangg.sayroam.global.websocket.StompSessionCloser;
import com.daebangg.sayroam.room.domain.error.RoomErrorCode;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

// 방 구독 권한은 구독 시점에만 확인하므로, 방을 나간 사용자의 다른 탭·기기에 남은 구독을 회수한다.
// 클라이언트가 알림을 받고 스스로 구독을 해제하는 것이 기본이고, 유예 시간 뒤에도 구독이 남은 세션만 끊는다.
@Slf4j
@Component
public class RoomSubscriptionRevoker {
    private static final String ROOM_NOTICE_QUEUE = "/queue/rooms";
    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    // 정상 클라이언트가 알림을 받고 구독을 해제하기까지 기다리는 시간
    private static final Duration UNSUBSCRIBE_GRACE = Duration.ofSeconds(5);

    private final SimpMessagingTemplate messagingTemplate;
    private final TaskScheduler taskScheduler;
    private final StompSessionCloser sessionCloser;

    public RoomSubscriptionRevoker(SimpMessagingTemplate messagingTemplate,
                                   @Qualifier("messageBrokerTaskScheduler") TaskScheduler taskScheduler,
                                   StompSessionCloser sessionCloser) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = taskScheduler;
        this.sessionCloser = sessionCloser;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRoomMemberLeft(RoomMemberLeft event) {
        notifyLeft(event);
        String roomTopic = ROOM_TOPIC_PREFIX + event.roomId();
        Set<String> roomDestinations = Set.of(roomTopic, roomTopic + "/members");
        // 유예 시간 안에 다시 참여해 구독한 세션도 끊길 수 있다. 재연결 후 다시 구독하면 권한 확인을 통과한다.
        taskScheduler.schedule(() -> sessionCloser.closeSessionsSubscribedTo(
                event.userId(), roomDestinations, RoomErrorCode.ROOM_ACCESS_DENIED), Instant.now().plus(UNSUBSCRIBE_GRACE));
    }

    // 리스너 예외는 이미 커밋된 요청의 응답을 실패로 바꾸므로 발송 실패는 로그만 남긴다. 남은 구독은 유예 뒤 끊긴다.
    private void notifyLeft(RoomMemberLeft event) {
        try {
            messagingTemplate.convertAndSendToUser(event.userId().toString(), ROOM_NOTICE_QUEUE,
                    RoomNotice.left(event.roomId()));
        } catch (RuntimeException exception) {
            log.warn("Failed to send room left notice: roomId={}, userId={}", event.roomId(), event.userId(), exception);
        }
    }
}
