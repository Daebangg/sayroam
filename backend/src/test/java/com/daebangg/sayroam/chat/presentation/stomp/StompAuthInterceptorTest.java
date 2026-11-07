package com.daebangg.sayroam.chat.presentation.stomp;

import com.daebangg.sayroam.auth.application.service.AccountAuthenticationService;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.room.application.RoomService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StompAuthInterceptorTest {
    private final StompAuthInterceptor interceptor = new StompAuthInterceptor(mock(JwtDecoder.class),
            mock(ActiveAccountJwtConverter.class), mock(RoomService.class), mock(AccountAuthenticationService.class));

    // 종료 예약이 실행되기 직전에 도착한 프레임도 만료 토큰이면 거부한다.
    @ParameterizedTest
    @CsvSource({"SEND, /app/rooms/1/messages", "SUBSCRIBE, /topic/rooms/1"})
    void 토큰이_만료된_연결의_프레임은_거부한다(StompCommand command, String destination) {
        // given
        ChatPrincipal expired = new ChatPrincipal(7L, 0L, Instant.now().minusSeconds(1));

        // when & then
        assertThatThrownBy(() -> interceptor.preSend(frame(command, destination, expired), mock(MessageChannel.class)))
                .isInstanceOf(StompAuthenticationException.class);
    }

    private Message<byte[]> frame(StompCommand command, String destination, ChatPrincipal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setDestination(destination);
        accessor.setUser(principal);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
