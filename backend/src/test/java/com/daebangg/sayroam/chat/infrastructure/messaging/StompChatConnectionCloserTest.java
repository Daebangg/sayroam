package com.daebangg.sayroam.chat.infrastructure.messaging;

import ch.qos.logback.classic.Level;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.global.logging.LogCapture;
import com.daebangg.sayroam.global.logging.WebSocketLog;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StompChatConnectionCloserTest {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void distinguishesAcceptedAndUnsentCloseRequests(boolean sent) {
        MessageChannel channel = mock(MessageChannel.class);
        when(channel.send(any())).thenReturn(sent);
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            new StompChatConnectionCloser(channel).close("session-123456", CommonErrorCode.ACCESS_DENIED);
            assertThat(capture.events()).hasSize(1);
            assertThat(capture.events().getFirst().getLevel()).isEqualTo(sent ? Level.INFO : Level.WARN);
            assertThat(capture.events().getFirst().getKeyValuePairs().toString())
                    .contains(sent ? "websocket_close_requested" : "websocket_close_not_sent", "ACCESS_DENIED", "session-123456");
        }
    }
}
