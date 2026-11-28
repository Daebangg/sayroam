package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

/** 표시할 필드만 고른다. 원본 이벤트에는 전체 세션 ID를 유지한다. */
public final class WebSocketLogConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        AnsiColor resultColor = event.getLevel().isGreaterOrEqual(Level.ERROR) ? AnsiColor.RED
                : event.getLevel().isGreaterOrEqual(Level.WARN) ? AnsiColor.YELLOW : AnsiColor.GREEN;
        StringBuilder result = new StringBuilder(AnsiOutput.toString(AnsiColor.CYAN, "WS ",
                resultColor, field(event, "action"), AnsiColor.DEFAULT));
        append(result, event, "route", " ", AnsiColor.BLUE, "");
        append(result, event, "errorCode", " · ", resultColor, "");
        append(result, event, "closeCode", " · ", resultColor, "");
        append(result, event, "durationMs", " · ", AnsiColor.MAGENTA, " ms");
        String sessionId = field(event, "sessionId");
        if (sessionId != null) result.append(" · session=").append(sessionId, 0, Math.min(8, sessionId.length()));
        String frames = field(event, "exceptionFrames");
        if (frames != null) result.append(System.lineSeparator()).append("    sessionId=").append(sessionId)
                .append(System.lineSeparator()).append("    ").append(frames);
        return result.toString();
    }

    private void append(StringBuilder result, ILoggingEvent event, String key, String separator, AnsiColor color, String suffix) {
        String value = field(event, key);
        if (value != null) result.append(separator).append(AnsiOutput.toString(color, value, suffix, AnsiColor.DEFAULT));
    }

    private String field(ILoggingEvent event, String name) {
        if (event.getKeyValuePairs() == null) return null;
        for (KeyValuePair pair : event.getKeyValuePairs()) {
            if (name.equals(pair.key) && pair.value != null) return pair.value.toString();
        }
        return null;
    }
}
