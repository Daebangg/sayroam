package com.daebangg.sayroam.global.logging;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoggingTaskDecoratorTest {
    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void passesOnlyTheRequestIdAndRestoresAReusedWorkerAfterFailure() throws Exception {
        LoggingTaskDecorator decorator = new LoggingTaskDecorator();
        AtomicReference<Map<String, String>> observed = new AtomicReference<>();
        MDC.put("requestId", "origin-request");
        MDC.put("token", "secret-token");
        Runnable task = decorator.decorate(() -> {
            observed.set(MDC.getCopyOfContextMap());
            throw new IllegalStateException("private-message");
        });
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            executor.submit(() -> {
                MDC.put("worker", "previous");
                assertThatThrownBy(task::run).isInstanceOf(IllegalStateException.class);
                assertThat(MDC.getCopyOfContextMap()).containsOnlyKeys("worker");
            }).get();
            assertThat(observed.get()).containsEntry("requestId", "origin-request")
                    .containsKey("taskId").doesNotContainKey("token");
            MDC.clear();
            Runnable unrelated = decorator.decorate(() -> observed.set(MDC.getCopyOfContextMap()));
            executor.submit(unrelated).get();
            assertThat(observed.get()).containsOnlyKeys("taskId");
        }
    }

    @Test
    void repeatingCallbacksReceiveANewTaskIdOnEveryExecution() {
        AtomicReference<String> taskId = new AtomicReference<>();
        Runnable task = new LoggingTaskDecorator().decorate(() -> taskId.set(MDC.get("taskId")));
        task.run();
        String first = taskId.get();
        task.run();
        assertThat(taskId.get()).isNotEqualTo(first);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void aDurableJobUsesItsOwnContextInsteadOfTheTriggeringRequest() {
        MDC.put("requestId", "different-request");
        try (LogContext ignored = LogContext.job(219)) {
            assertThat(MDC.get("requestId")).isNull();
            assertThat(MDC.get("jobId")).isEqualTo("219");
            assertThat(MDC.get("taskId")).isNotBlank();
        }
        assertThat(MDC.get("requestId")).isEqualTo("different-request");
    }
}
