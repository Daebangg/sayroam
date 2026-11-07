package com.daebangg.sayroam.auth.infrastructure.config;

import com.daebangg.sayroam.auth.application.model.SocialAuthPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class SocialAuthConfig {
    @Bean
    SocialAuthPolicy socialAuthPolicy(SocialAuthProperties properties) {
        return new SocialAuthPolicy(Duration.ofSeconds(properties.transactionTtlSeconds()),
                Duration.ofSeconds(properties.resultTtlSeconds()), Duration.ofSeconds(properties.signupTtlSeconds()),
                properties.allowedReturnPaths());
    }
}
