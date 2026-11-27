package com.daebangg.sayroam.chat.presentation.stomp;

import ch.qos.logback.classic.Level;
import com.daebangg.sayroam.global.logging.LogCapture;
import com.daebangg.sayroam.global.logging.WebSocketLog;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

class StompErrorHandlerTest {
    @Test
    void logsAuthenticationAndAuthorizationRejectionsWithoutPrivateData() {
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            reject(new StompAuthenticationException());
            reject(new StompAccessDeniedException());
            assertThat(capture.events()).hasSize(2).allSatisfy(event -> {
                assertThat(event.getLevel()).isEqualTo(Level.WARN);
                assertThat(event.getThrowableProxy()).isNull();
                assertThat(event.getKeyValuePairs().toString()).doesNotContain("private-token", "private-body", "private-destination");
            });
            assertThat(capture.events().getFirst().getKeyValuePairs().toString()).contains("UNAUTHENTICATED");
            assertThat(capture.events().getLast().getKeyValuePairs().toString()).contains("ACCESS_DENIED");
        }
    }

    @Test
    void unexpectedFailuresKeepSafeDiagnosticsWithoutExceptionMessage() {
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            reject(new IllegalStateException("private-exception-message"));
            assertThat(capture.events()).hasSize(1);
            assertThat(capture.events().getFirst().getLevel()).isEqualTo(Level.ERROR);
            assertThat(capture.events().getFirst().getKeyValuePairs().toString())
                    .contains("INTERNAL_SERVER_ERROR", "exceptionFrames", "IllegalStateException")
                    .doesNotContain("private-exception-message", "private-token", "private-body");
        }
    }

    private void reject(Throwable failure) {
        StompHeaderAccessor client = StompHeaderAccessor.create(StompCommand.CONNECT);
        client.setSessionId("session-123456");
        client.setDestination("/private-destination");
        client.setNativeHeader("Authorization", "Bearer private-token");
        client.setLeaveMutable(true);
        StompHeaderAccessor error = StompHeaderAccessor.create(StompCommand.ERROR);
        Message<byte[]> response = new StompErrorHandler().handleInternal(error, new byte[0], failure, client);
        assertThat(StompHeaderAccessor.wrap(response).getMessage()).isNotBlank();
    }
}
