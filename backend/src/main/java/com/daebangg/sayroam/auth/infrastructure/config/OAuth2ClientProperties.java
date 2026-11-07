package com.daebangg.sayroam.auth.infrastructure.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/** 공급자는 같은 설정 형식을 사용하며 활성화한 클라이언트만 등록한다. */
@Validated
@ConfigurationProperties("app.auth.social")
public record OAuth2ClientProperties(@DefaultValue @Valid Client google,
                                     @DefaultValue @Valid Client kakao,
                                     @DefaultValue @Valid Client naver) {
    public record Client(@DefaultValue("false") boolean enabled, String clientId, String clientSecret, URI redirectUri) {
        @AssertTrue(message = "An enabled OAuth2 provider requires client-id, client-secret and redirect-uri")
        public boolean isConfigured() {
            return !enabled || (clientId != null && !clientId.isBlank()
                    && clientSecret != null && !clientSecret.isBlank() && redirectUri != null);
        }
        @Override public String toString() { return "OAuth2Client[redacted]"; }
    }
    @Override public String toString() { return "OAuth2ClientProperties[redacted]"; }
}
