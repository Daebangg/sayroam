package com.daebangg.sayroam.companion.presentation.scheduling;

import com.daebangg.sayroam.global.logging.LoggingTaskDecorator;
import com.daebangg.sayroam.global.logging.SafeLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@Slf4j
@EnableScheduling
@ConditionalOnProperty(prefix = "app.companion.matching", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CompanionMatchingConfig {
    @Bean(defaultCandidate = false)
    public ThreadPoolTaskExecutor companionMatchingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("companion-matching-");
        executor.setTaskDecorator(new LoggingTaskDecorator());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Bean(defaultCandidate = false)
    public ThreadPoolTaskScheduler companionMatchingRecoveryScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("companion-matching-recovery-");
        scheduler.setTaskDecorator(new LoggingTaskDecorator());
        scheduler.setErrorHandler(exception -> SafeLog.atError(log, exception)
                .addKeyValue("event", "matching_recovery_failed").log("Matching recovery failed"));
        return scheduler;
    }
}
