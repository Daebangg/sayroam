package com.daebangg.sayroam.global.logging;

import org.slf4j.Logger;
import org.slf4j.spi.LoggingEventBuilder;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** 예외 메시지·suppressed·Throwable 원문 없이 유형과 호출 위치만 기록한다. */
public final class SafeLog {
    private static final int MAX_CAUSES = 4;
    private static final int MAX_FRAMES = 8;

    private SafeLog() {
    }

    public static LoggingEventBuilder atWarn(Logger logger, Throwable failure) {
        return withFailure(logger.atWarn(), failure);
    }

    public static LoggingEventBuilder atError(Logger logger, Throwable failure) {
        return withFailure(logger.atError(), failure);
    }

    public static LoggingEventBuilder withFailure(LoggingEventBuilder event, Throwable failure) {
        if (failure == null) return event;
        return event.addKeyValue("exceptionType", failure.getClass().getName())
                .addKeyValue("exceptionFrames", frames(failure));
    }

    private static String frames(Throwable failure) {
        StringBuilder result = new StringBuilder();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = failure;
        for (int cause = 0; current != null && cause < MAX_CAUSES && seen.add(current); cause++) {
            if (!result.isEmpty()) result.append(" <- ");
            result.append(current.getClass().getName());
            StackTraceElement[] stack = current.getStackTrace();
            for (int index = 0; index < Math.min(stack.length, MAX_FRAMES); index++) {
                StackTraceElement frame = stack[index];
                result.append(" at ").append(frame.getClassName()).append('.').append(frame.getMethodName())
                        .append(':').append(frame.getLineNumber());
            }
            current = current.getCause();
        }
        return result.toString();
    }
}
