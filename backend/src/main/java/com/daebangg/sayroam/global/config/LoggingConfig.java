package com.daebangg.sayroam.global.config;

import com.daebangg.sayroam.global.logging.LoggingTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;

@Configuration(proxyBeanMethods = false)
public class LoggingConfig {
    // Boot의 기본 applicationTaskExecutor(MVC Callable 포함)가 이 decorator를 사용한다.
    @Bean
    public TaskDecorator loggingTaskDecorator() {
        return new LoggingTaskDecorator();
    }
}
