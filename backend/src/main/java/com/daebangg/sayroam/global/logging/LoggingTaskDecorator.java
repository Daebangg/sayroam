package com.daebangg.sayroam.global.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/** MDC 전체나 인증 정보를 복사하지 않고 요청 ID만 전달한다. 반복 실행에도 새 작업 ID를 부여한다. */
public final class LoggingTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
        String requestId = MDC.get("requestId");
        return () -> {
            try (LogContext ignored = LogContext.task(requestId)) {
                runnable.run();
            }
        };
    }
}
