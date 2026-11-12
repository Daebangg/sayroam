package com.daebangg.sayroam.auth.infrastructure.identity;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Validated
@ConfigurationProperties("app.auth.social")
public record SocialAuthProperties(
        @DefaultValue("300") @Min(60) @Max(600) int transactionTtlSeconds,
        @DefaultValue("60") @Min(30) @Max(120) int resultTtlSeconds,
        @DefaultValue("600") @Min(60) @Max(1800) int signupTtlSeconds,
        @DefaultValue("/") Set<String> allowedReturnPaths,
        java.net.URI frontendOrigin) { }
