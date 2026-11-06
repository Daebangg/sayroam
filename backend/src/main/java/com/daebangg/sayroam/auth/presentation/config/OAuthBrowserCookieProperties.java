package com.daebangg.sayroam.auth.presentation.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("app.auth.social.browser-cookie")
public record OAuthBrowserCookieProperties(@DefaultValue("true") boolean secure,
        @DefaultValue("3600") @Min(3000) @Max(7200) int maxAgeSeconds) { }
