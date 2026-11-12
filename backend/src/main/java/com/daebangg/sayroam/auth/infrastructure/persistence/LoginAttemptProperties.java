package com.daebangg.sayroam.auth.infrastructure.persistence;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth.login-attempts")
public record LoginAttemptProperties(
        @DefaultValue("5") @Min(1) @Max(100) int maxFailures,
        @DefaultValue("100") @Min(1) @Max(100000) int ipMaxFailures,
        @DefaultValue("300") @Min(1) @Max(86400) int windowSeconds,
        @DefaultValue("300") @Min(1) @Max(86400) int blockSeconds) { }
