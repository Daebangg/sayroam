package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.slf4j.LoggerFactory;

import java.util.List;

/** MDC가 정리되기 전에 이벤트를 고정한다. 테스트 종료 시 기존 로거 설정을 복구한다. */
public final class LogCapture implements AutoCloseable {
    private final Logger logger;
    private final Level previous;
    private final ListAppender<ILoggingEvent> appender = new ListAppender<>() {
        @Override
        protected void append(ILoggingEvent event) {
            event.prepareForDeferredProcessing();
            super.append(event);
        }
    };

    public LogCapture(Class<?> source) {
        logger = (Logger) LoggerFactory.getLogger(source);
        previous = logger.getLevel();
        logger.setLevel(Level.DEBUG);
        appender.list = new java.util.concurrent.CopyOnWriteArrayList<>();
        appender.start();
        logger.addAppender(appender);
    }

    public List<ILoggingEvent> events() {
        return appender.list;
    }

    @Override
    public void close() {
        logger.detachAppender(appender);
        logger.setLevel(previous);
        appender.stop();
    }
}
