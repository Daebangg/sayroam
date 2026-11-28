package com.daebangg.sayroam.global.logging;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** 요청·작업 범위에 허용한 식별자만 설치하고, 종료 시 실행 스레드의 원래 상태를 복구한다. */
public final class LogContext implements AutoCloseable {
    private final Map<String, String> previous;

    private LogContext(Map<String, String> context) {
        previous = MDC.getCopyOfContextMap();
        MDC.setContextMap(context);
    }

    public static LogContext request(String requestId) {
        return new LogContext(Map.of("requestId", requestId));
    }

    public static LogContext task(String requestId) {
        Map<String, String> context = new HashMap<>();
        context.put("taskId", UUID.randomUUID().toString());
        if (requestId != null) context.put("requestId", requestId);
        return new LogContext(context);
    }

    /** 여러 요청의 작업을 묶어 처리하는 워커에서는 시작 신호의 요청 ID를 다른 작업에 붙이지 않는다. */
    public static LogContext job(long jobId) {
        return new LogContext(Map.of("taskId", UUID.randomUUID().toString(), "jobId", Long.toString(jobId)));
    }

    @Override
    public void close() {
        if (previous == null) MDC.clear();
        else MDC.setContextMap(previous);
    }
}
