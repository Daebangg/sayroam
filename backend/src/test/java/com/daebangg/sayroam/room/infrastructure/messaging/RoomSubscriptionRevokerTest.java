package com.daebangg.sayroam.room.infrastructure.messaging;

import com.daebangg.sayroam.global.websocket.StompPrincipal;
import com.daebangg.sayroam.global.websocket.StompSessionCloser;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

class RoomSubscriptionRevokerTest {
    private static final Long USER_ID = 7L;
    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-10T10:00:00Z");

    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final TaskScheduler taskScheduler = mock(TaskScheduler.class);
    private final DefaultSimpUserRegistry userRegistry = new DefaultSimpUserRegistry();
    private final List<Message<?>> sentFrames = new ArrayList<>();
    private final RoomSubscriptionRevoker revoker = new RoomSubscriptionRevoker(messagingTemplate, taskScheduler,
            new StompSessionCloser((message, timeout) -> sentFrames.add(message), userRegistry));

    @Test
    void 방을_나가면_그_회원의_개인_큐로_나간_방을_알린다() {
        // when
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));

        // then
        then(messagingTemplate).should().convertAndSendToUser("7", "/queue/rooms", RoomNotice.left(ROOM_ID));
    }

    @Test
    void 방을_나가면_바로_끊지_않고_유예_시간_뒤로_구독_확인을_예약한다() {
        // given
        connect("phone");
        subscribe("phone", "/topic/rooms/" + ROOM_ID);
        Instant before = Instant.now();

        // when
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));

        // then
        ArgumentCaptor<Instant> startTime = ArgumentCaptor.forClass(Instant.class);
        then(taskScheduler).should().schedule(any(Runnable.class), startTime.capture());
        assertThat(startTime.getValue()).isAfterOrEqualTo(before.plus(Duration.ofSeconds(5)));
        assertThat(sentFrames).isEmpty();
    }

    @Test
    void 유예_시간_뒤에도_그_방을_구독한_세션은_방_접근_거부_코드로_종료한다() {
        // given
        connect("phone");
        subscribe("phone", "/topic/rooms/" + ROOM_ID);

        // when
        leaveAndRunScheduledCheck();

        // then
        assertThat(closedSessionIds()).containsExactly("phone");
        assertThat(closeReasons()).containsExactly("ROOM_ACCESS_DENIED");
    }

    @Test
    void 유예_시간_뒤에도_그_방의_멤버_채널을_구독한_세션은_종료한다() {
        // given
        connect("phone");
        subscribe("phone", "/topic/rooms/" + ROOM_ID + "/members");

        // when
        leaveAndRunScheduledCheck();

        // then
        assertThat(closedSessionIds()).containsExactly("phone");
    }

    @Test
    void 유예_시간_안에_구독을_해제한_세션은_유지한다() {
        // given
        connect("phone");
        subscribe("phone", "/topic/rooms/" + ROOM_ID);
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));
        unsubscribe("phone", "/topic/rooms/" + ROOM_ID);

        // when
        runScheduledCheck();

        // then
        assertThat(sentFrames).isEmpty();
    }

    @Test
    void 다른_방만_구독한_세션은_유지한다() {
        // given
        connect("phone");
        subscribe("phone", "/topic/rooms/" + ROOM_ID);
        connect("laptop");
        subscribe("laptop", "/topic/rooms/" + OTHER_ROOM_ID);

        // when
        leaveAndRunScheduledCheck();

        // then
        assertThat(closedSessionIds()).containsExactly("phone");
    }

    private void leaveAndRunScheduledCheck() {
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));
        runScheduledCheck();
    }

    private void runScheduledCheck() {
        ArgumentCaptor<Runnable> check = ArgumentCaptor.forClass(Runnable.class);
        then(taskScheduler).should().schedule(check.capture(), any(Instant.class));
        check.getValue().run();
    }

    private void connect(String sessionId) {
        userRegistry.onApplicationEvent(new SessionConnectedEvent(this, sessionMessage(sessionId, null, null),
                new StompPrincipal(USER_ID, 0L, EXPIRES_AT)));
    }

    private void subscribe(String sessionId, String destination) {
        userRegistry.onApplicationEvent(new SessionSubscribeEvent(this,
                sessionMessage(sessionId, "sub-" + destination, destination)));
    }

    private void unsubscribe(String sessionId, String destination) {
        userRegistry.onApplicationEvent(new SessionUnsubscribeEvent(this,
                sessionMessage(sessionId, "sub-" + destination, null)));
    }

    private Message<byte[]> sessionMessage(String sessionId, String subscriptionId, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId(subscriptionId);
        accessor.setDestination(destination);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private List<String> closedSessionIds() {
        return sentFrames.stream()
                .map(frame -> SimpMessageHeaderAccessor.getSessionId(frame.getHeaders()))
                .toList();
    }

    private List<String> closeReasons() {
        return sentFrames.stream()
                .map(frame -> StompHeaderAccessor.wrap(frame).getMessage())
                .toList();
    }
}
