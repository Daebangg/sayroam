package com.daebangg.sayroam.global.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;
import org.slf4j.spi.LoggingEventBuilder;

/** 소켓 이벤트의 구조화 필드와 로컬 전용 콘솔 출력을 한 로거로 모은다. */
public final class WebSocketLog {
    private static final Logger LOG = LoggerFactory.getLogger(WebSocketLog.class);

    private WebSocketLog() { }

    public static LoggingEventBuilder event(Level level, String event, String action, String sessionId) {
        return LOG.atLevel(level).addKeyValue("event", event).addKeyValue("action", action)
                .addKeyValue("sessionId", sessionId == null ? "none" : sessionId);
    }
}
