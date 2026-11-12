package com.daebangg.sayroam.auth.infrastructure.security;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth.access-token")
public record AccessTokenProperties(
        String secretBase64,
        @DefaultValue("sayroam") @NotBlank String issuer,
        @DefaultValue("sayroam-api") @NotBlank String audience,
        @DefaultValue("900") @Min(60) @Max(900) int ttlSeconds) {
    @Override
    public String toString() {
        return "AccessTokenProperties[redacted]";
    }
}
