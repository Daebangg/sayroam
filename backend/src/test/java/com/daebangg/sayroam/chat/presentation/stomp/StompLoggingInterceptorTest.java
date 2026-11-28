package com.daebangg.sayroam.chat.presentation.stomp;

import ch.qos.logback.classic.Level;
import com.daebangg.sayroam.global.logging.LogCapture;
import com.daebangg.sayroam.global.logging.WebSocketLog;
import com.daebangg.sayroam.global.logging.WebSocketLogConverter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class StompLoggingInterceptorTest {
    private final StompLoggingInterceptor interceptor = new StompLoggingInterceptor();
    private final ChatPrincipal principal = new ChatPrincipal(7L, 0L, Instant.now().plusSeconds(60));

    @Test
    void recordsConnectionAndAbnormalCloseOnceWithoutClientReasonOrPayload() {
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            Message<byte[]> message = frame(StompCommand.CONNECTED, null);
            interceptor.onConnected(new SessionConnectedEvent(this, message, principal));
            interceptor.onConnected(new SessionConnectedEvent(this, message, principal));
            SessionDisconnectEvent close = new SessionDisconnectEvent(this, message, "session-123456",
                    CloseStatus.SERVER_ERROR.withReason("private-close-reason"), principal);
            interceptor.onDisconnected(close);
            interceptor.onDisconnected(close);

            assertThat(capture.events()).hasSize(2);
            assertThat(capture.events().getFirst().getLevel()).isEqualTo(Level.INFO);
            assertThat(capture.events().getLast().getLevel()).isEqualTo(Level.WARN);
            assertThat(capture.events().getLast().getKeyValuePairs().toString())
                    .contains("websocket_disconnected", "session-123456", "1011", "durationMs")
                    .doesNotContain("private-close-reason", "private-body", "private-token");
        }
    }

    @Test
    void debugFramesUseTemplatesAndExcludeHeadersAndBody() {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            MessageChannel channel = mock(MessageChannel.class);
            interceptor.postSend(frame(StompCommand.SEND, "/app/rooms/987/messages"), channel, true);
            interceptor.postSend(frame(StompCommand.SUBSCRIBE, "/topic/rooms/987/members"), channel, true);
            interceptor.postSend(frame(StompCommand.UNSUBSCRIBE, null), channel, true);
            interceptor.postSend(frame(StompCommand.SEND, "/private-destination"), channel, true);
            interceptor.postSend(frame(StompCommand.SEND, "/private-destination"), channel, false);

            assertThat(capture.events()).hasSize(4).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.DEBUG);
                assertThat(event.getKeyValuePairs().toString()).doesNotContain("987", "private-destination", "private-token", "private-body");
                assertThat(event.getFormattedMessage()).doesNotContain("private-destination", "private-token", "private-body");
                assertThat(event.getArgumentArray()).isNullOrEmpty();
                assertThat(event.getMDCPropertyMap().toString()).doesNotContain("private-token", "private-body");
                assertThat(event.getThrowableProxy()).isNull();
            });
            WebSocketLogConverter converter = new WebSocketLogConverter();
            assertThat(converter.convert(capture.events().getFirst()))
                    .isEqualTo("WS SEND /app/rooms/{roomId}/messages · session=session-");
            assertThat(capture.events().stream().map(converter::convert).toList().toString())
                    .contains("/topic/rooms/{roomId}/members", "UNMAPPED")
                    .doesNotContain("987", "private-destination", "private-token", "private-body");
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }

    private Message<byte[]> frame(StompCommand command, String destination) {
        StompHeaderAccessor headers = StompHeaderAccessor.create(command);
        headers.setSessionId("session-123456");
        if (destination != null) headers.setDestination(destination);
        headers.setNativeHeader("Authorization", "Bearer private-token");
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage("private-body".getBytes(java.nio.charset.StandardCharsets.UTF_8), headers.getMessageHeaders());
    }
}
