package com.daebangg.sayroam.auth.infrastructure.identity;

import com.daebangg.sayroam.auth.domain.SocialAuthSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class SocialAuthConfig {
    @Bean
    SocialAuthSettings socialAuthSettings(SocialAuthProperties properties) {
        return new SocialAuthSettings(Duration.ofSeconds(properties.transactionTtlSeconds()),
                Duration.ofSeconds(properties.resultTtlSeconds()), Duration.ofSeconds(properties.signupTtlSeconds()),
                properties.allowedReturnPaths(), properties.frontendOrigin());
    }
}
