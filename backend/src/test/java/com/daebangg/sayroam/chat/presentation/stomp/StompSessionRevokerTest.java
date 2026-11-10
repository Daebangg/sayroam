package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.domain.event.LoginsInvalidated;
import com.daebangg.sayroam.room.domain.event.RoomMemberLeft;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.messaging.DefaultSimpUserRegistry;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class StompSessionRevokerTest {
    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 8L;
    private static final Long ROOM_ID = 1L;
    private static final Long OTHER_ROOM_ID = 2L;
    private static final Instant EXPIRES_AT = Instant.parse("2026-11-10T10:00:00Z");

    private final DefaultSimpUserRegistry userRegistry = new DefaultSimpUserRegistry();
    private final List<Message<?>> sentFrames = new ArrayList<>();
    private final StompSessionRevoker revoker = new StompSessionRevoker(userRegistry,
            new StompSessionCloser((message, timeout) -> sentFrames.add(message)));

    @Test
    void 로그인이_무효화되면_그_회원의_모든_세션을_종료한다() {
        // given
        connect("phone", USER_ID);
        connect("laptop", USER_ID);

        // when
        revoker.onLoginsInvalidated(new LoginsInvalidated(USER_ID));

        // then
        assertThat(closedSessionIds()).containsExactlyInAnyOrder("phone", "laptop");
    }

    @Test
    void 로그인이_무효화되면_인증_오류_코드로_종료한다() {
        // given
        connect("phone", USER_ID);

        // when
        revoker.onLoginsInvalidated(new LoginsInvalidated(USER_ID));

        // then
        assertThat(closeReasons()).containsExactly("UNAUTHENTICATED");
    }

    @Test
    void 방을_나가면_그_방을_구독한_세션을_방_접근_거부_코드로_종료한다() {
        // given
        connect("phone", USER_ID);
        subscribe("phone", "/topic/rooms/" + ROOM_ID);

        // when
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));

        // then
        assertThat(closedSessionIds()).containsExactly("phone");
        assertThat(closeReasons()).containsExactly("ROOM_ACCESS_DENIED");
    }

    @Test
    void 방을_나가도_다른_방만_구독한_세션은_유지한다() {
        // given
        connect("phone", USER_ID);
        subscribe("phone", "/topic/rooms/" + ROOM_ID);
        connect("laptop", USER_ID);
        subscribe("laptop", "/topic/rooms/" + OTHER_ROOM_ID);

        // when
        revoker.onRoomMemberLeft(new RoomMemberLeft(ROOM_ID, USER_ID));

        // then
        assertThat(closedSessionIds()).containsExactly("phone");
    }

    @Test
    void 다른_회원의_세션은_종료하지_않는다() {
        // given
        connect("mine", USER_ID);
        connect("others", OTHER_USER_ID);

        // when
        revoker.onLoginsInvalidated(new LoginsInvalidated(USER_ID));

        // then
        assertThat(closedSessionIds()).containsExactly("mine");
    }

    @Test
    void 연결된_세션이_없으면_아무것도_하지_않는다() {
        // when
        revoker.onLoginsInvalidated(new LoginsInvalidated(USER_ID));

        // then
        assertThat(sentFrames).isEmpty();
    }

    @Test
    void 세션_종료에_실패해도_예외를_전파하지_않는다() {
        // given
        StompSessionRevoker failingRevoker = new StompSessionRevoker(userRegistry,
                new StompSessionCloser((message, timeout) -> {
                    throw new IllegalStateException("closed channel");
                }));
        connect("phone", USER_ID);

        // when & then
        assertThatCode(() -> failingRevoker.onLoginsInvalidated(new LoginsInvalidated(USER_ID)))
                .doesNotThrowAnyException();
    }

    private void connect(String sessionId, Long userId) {
        userRegistry.onApplicationEvent(new SessionConnectedEvent(this, sessionMessage(sessionId),
                new ChatPrincipal(userId, 0L, EXPIRES_AT)));
    }

    private void subscribe(String sessionId, String destination) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        accessor.setSubscriptionId("sub-" + destination);
        accessor.setDestination(destination);
        userRegistry.onApplicationEvent(new SessionSubscribeEvent(this,
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders())));
    }

    private Message<byte[]> sessionMessage(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
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
