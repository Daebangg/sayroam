package com.daebangg.sayroam.companion.presentation.scheduling;

import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.companion.application.service.CompanionMatchingService;
import com.daebangg.sayroam.global.logging.LogCapture;
import org.slf4j.MDC;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CompanionMatchingWorkerTest {
    @Test
    void failedJobsUseTheirOwnIdWithoutTheTriggeringRequestOrClaimToken() {
        CompanionMatchingService matching = mock(CompanionMatchingService.class);
        CompanionMatchingClaim claim = new CompanionMatchingClaim(219, "private-claim-token");
        Queue<Runnable> executions = new ArrayDeque<>();
        CompanionMatchingWorker worker = new CompanionMatchingWorker(matching, executions::add);
        when(matching.claim()).thenReturn(Optional.of(claim)).thenReturn(Optional.empty());
        doThrow(new IllegalStateException("private-email@example.test private-claim-token")).when(matching).process(claim);
        MDC.put("requestId", "unrelated-trigger");
        try (LogCapture capture = new LogCapture(CompanionMatchingWorker.class)) {
            worker.requestRun();
            executions.remove().run();
            assertThat(capture.events()).hasSize(1);
            assertThat(capture.events().getFirst().getMDCPropertyMap()).containsEntry("jobId", "219")
                    .containsKey("taskId").doesNotContainKey("requestId");
            assertThat(capture.events().getFirst().getKeyValuePairs().toString())
                    .doesNotContain("private-email", "private-claim-token");
            assertThat(MDC.get("requestId")).isEqualTo("unrelated-trigger");
            verify(matching).retry(claim);
        } finally {
            MDC.clear();
        }
    }

    @Test
    void coalescesBurstsAndContinuesAfterTenBatchesWithoutWaitingForRecovery() {
        CompanionMatchingService matching = mock(CompanionMatchingService.class);
        Queue<Runnable> executions = new ArrayDeque<>();
        CompanionMatchingWorker worker = new CompanionMatchingWorker(matching, executions::add);
        CompanionMatchingClaim claim = new CompanionMatchingClaim(1L, "token");
        AtomicInteger claims = new AtomicInteger();
        when(matching.claim()).thenAnswer(invocation -> claims.incrementAndGet() <= 11
                ? Optional.of(claim) : Optional.empty());

        for (int event = 0; event < 100; event++) worker.requestRun();
        assertThat(executions).hasSize(1);
        executions.remove().run();
        verify(matching, times(10)).process(claim);
        assertThat(executions).hasSize(1);
        executions.remove().run();
        verify(matching, times(11)).process(claim);
        assertThat(executions).isEmpty();
    }

    @Test
    void preservesAWakeupArrivingWhenTheWorkerFindsNoMoreJobs() {
        CompanionMatchingService matching = mock(CompanionMatchingService.class);
        Queue<Runnable> executions = new ArrayDeque<>();
        CompanionMatchingWorker worker = new CompanionMatchingWorker(matching, executions::add);
        AtomicInteger claims = new AtomicInteger();
        when(matching.claim()).thenAnswer(invocation -> {
            if (claims.incrementAndGet() == 1) worker.requestRun();
            return Optional.empty();
        });
        worker.requestRun();
        executions.remove().run();
        assertThat(executions).hasSize(1);
        executions.remove().run();
        assertThat(executions).isEmpty();
        assertThat(claims).hasValue(2);
    }

    @Test
    void canDispatchAgainAfterTheExecutorRejectedASignal() {
        CompanionMatchingService matching = mock(CompanionMatchingService.class);
        TaskExecutor executor = mock(TaskExecutor.class);
        CompanionMatchingWorker worker = new CompanionMatchingWorker(matching, executor);
        doThrow(new TaskRejectedException("rejected")).doNothing().when(executor).execute(any(Runnable.class));
        worker.requestRun();
        worker.recover();
        verify(executor, times(2)).execute(any(Runnable.class));
    }
}
