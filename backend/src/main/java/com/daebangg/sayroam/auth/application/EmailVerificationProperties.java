package com.daebangg.sayroam.auth.application;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth.email-verification")
public record EmailVerificationProperties(
        @DefaultValue("300") @Min(60) @Max(900) int codeTtlSeconds,
        @DefaultValue("60") @Min(1) @Max(900) int resendCooldownSeconds,
        @DefaultValue("3600") @Min(60) @Max(86400) int rateWindowSeconds,
        @DefaultValue("5") @Min(1) @Max(100) int maxRequestsPerEmail,
        @DefaultValue("20") @Min(1) @Max(1000) int maxRequestsPerIp) {
}
