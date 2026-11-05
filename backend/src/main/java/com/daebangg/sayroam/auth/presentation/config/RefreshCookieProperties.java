package com.daebangg.sayroam.auth.presentation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** 브라우저 전송 정책. Redis 저장 정책과 분리한다. */
@ConfigurationProperties("app.auth.refresh-cookie")
public record RefreshCookieProperties(@DefaultValue("true") boolean secure) {
}
