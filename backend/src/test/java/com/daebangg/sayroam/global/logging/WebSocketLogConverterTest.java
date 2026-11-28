package com.daebangg.sayroam.global.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;
import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketLogConverterTest {
    @Test
    void rendersCompactColoredEventsAndPreservesFullSessionIdInOriginalEvent() {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        try (LogCapture capture = new LogCapture(WebSocketLog.class)) {
            WebSocketLog.event(Level.INFO, "websocket_disconnected", "DISCONNECTED", "abcdefgh-full-session-id")
                    .addKeyValue("closeCode", 1000).addKeyValue("durationMs", 42).log("WebSocket disconnected");
            String output = new WebSocketLogConverter().convert(capture.events().getFirst());
            assertThat(output).contains("\u001B[36mWS ", "\u001B[32mDISCONNECTED", "\u001B[35m42 ms", "session=abcdefgh");
            assertThat(output.replaceAll("\u001B\\[[;\\d]*m", ""))
                    .isEqualTo("WS DISCONNECTED · 1000 · 42 ms · session=abcdefgh");
            assertThat(capture.events().getFirst().getKeyValuePairs().toString()).contains("abcdefgh-full-session-id");
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }
}
