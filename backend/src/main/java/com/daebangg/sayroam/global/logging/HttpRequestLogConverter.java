package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.slf4j.event.KeyValuePair;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;

/** 로컬 화면에만 요청 결과를 간결하게 표시한다. 원본 필드·MDC와 운영 JSON은 변경하지 않는다. */
public final class HttpRequestLogConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent event) {
        if (!"http_request_completed".equals(field(event, "event"))) return event.getFormattedMessage();

        String method = text(field(event, "method"));
        String route = text(field(event, "route"));
        Object status = field(event, "status");
        String duration = text(field(event, "durationMs"));
        String result = AnsiOutput.toString(
                AnsiColor.CYAN, method, AnsiColor.DEFAULT, " ",
                AnsiColor.BLUE, route, AnsiColor.DEFAULT, " → ",
                statusColor(status), text(status), AnsiColor.DEFAULT, " · ",
                AnsiColor.MAGENTA, duration, " ms", AnsiColor.DEFAULT);

        Object errorCode = field(event, "errorCode");
        if (errorCode != null) result += " · " + errorCode;

        // 평소에는 숨기고, 예기치 않은 장애의 상세 줄에서만 요청 ID와 안전한 호출 위치를 보여준다.
        Object frames = field(event, "exceptionFrames");
        if (frames != null) {
            String requestId = event.getMDCPropertyMap().get("requestId");
            if (requestId != null) result += System.lineSeparator() + "    requestId=" + requestId;
            result += System.lineSeparator() + "    " + frames;
        }
        return result;
    }

    private Object field(ILoggingEvent event, String name) {
        if (event.getKeyValuePairs() == null) return null;
        for (KeyValuePair pair : event.getKeyValuePairs()) {
            if (name.equals(pair.key)) return pair.value;
        }
        return null;
    }

    private String text(Object value) {
        return value == null ? "?" : value.toString();
    }

    private AnsiColor statusColor(Object value) {
        if (!(value instanceof Number status)) return AnsiColor.DEFAULT;
        if (status.intValue() >= 500) return AnsiColor.RED;
        if (status.intValue() >= 400) return AnsiColor.YELLOW;
        return AnsiColor.GREEN;
    }
}
