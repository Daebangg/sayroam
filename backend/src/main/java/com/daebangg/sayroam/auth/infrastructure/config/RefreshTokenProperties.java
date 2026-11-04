package com.daebangg.sayroam.auth.infrastructure.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth.refresh-token")
public record RefreshTokenProperties(
        @DefaultValue("604800") @Min(3600) @Max(2592000) int ttlSeconds,
        @DefaultValue("10000") @Min(1) @Max(10000) int maxRotations) {
}
