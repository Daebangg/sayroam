package com.daebangg.sayroam.companion.presentation.scheduling;

import com.daebangg.sayroam.companion.application.dto.CompanionMatchingClaim;
import com.daebangg.sayroam.companion.application.service.CompanionMatchingService;
import com.daebangg.sayroam.companion.domain.event.CompanionMatchingRequested;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(prefix = "app.companion.matching", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class CompanionMatchingWorker {
    private static final int MAX_BATCHES_PER_RUN = 10;

    private final CompanionMatchingService matching;
    private final TaskExecutor executor;
    private final AtomicBoolean requested = new AtomicBoolean();
    private final AtomicBoolean scheduled = new AtomicBoolean();

    public CompanionMatchingWorker(CompanionMatchingService matching,
                                   @Qualifier("companionMatchingExecutor") TaskExecutor executor) {
        this.matching = matching;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMatchingRequested(CompanionMatchingRequested event) {
        requestRun();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        requestRun();
    }

    @Scheduled(fixedDelayString = "${app.companion.matching.recovery-delay-ms:30000}",
            initialDelayString = "${app.companion.matching.recovery-delay-ms:30000}",
            scheduler = "companionMatchingRecoveryScheduler")
    public void recover() {
        requestRun();
    }

    public void requestRun() {
        requested.set(true);
        // 이벤트가 몰려도 실제 작업은 DB에 두고, 메모리에는 실행 신호 한 건만 유지한다.
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(this::run);
        } catch (TaskRejectedException exception) {
            scheduled.set(false);
            // 커밋된 작업은 DB에 남아 시작 시 확인 또는 주기적 복구에서 처리한다.
            log.warn("Companion matching dispatch failed: {}", exception.getClass().getSimpleName());
        }
    }

    private void run() {
        boolean hasMore = false;
        try {
            requested.set(false);
            hasMore = drain();
        } finally {
            scheduled.set(false);
            // 실행 종료 직전 도착한 이벤트와 10배치 뒤 남은 작업을 즉시 이어서 처리한다.
            if (hasMore || requested.get()) {
                requestRun();
            }
        }
    }

    private boolean drain() {
        for (int batch = 0; batch < MAX_BATCHES_PER_RUN; batch++) {
            Optional<CompanionMatchingClaim> next;
            try {
                next = matching.claim();
            } catch (RuntimeException exception) {
                log.warn("Companion matching claim failed: {}", exception.getClass().getSimpleName());
                return false;
            }
            if (next.isEmpty()) {
                return false;
            }
            CompanionMatchingClaim claim = next.get();
            try {
                matching.process(claim);
            } catch (RuntimeException exception) {
                log.warn("Companion matching job {} failed: {}", claim.jobId(), exception.getClass().getSimpleName());
                try {
                    matching.retry(claim);
                } catch (RuntimeException retryException) {
                    log.warn("Companion matching retry {} failed: {}", claim.jobId(), retryException.getClass().getSimpleName());
                }
            }
        }
        return true;
    }
}
