package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.net.URI;

public record SocialAuthStartResponse(
        @Schema(description = "응답을 받은 같은 브라우저에서 이동할 공급자 인증 주소", requiredMode = Schema.RequiredMode.REQUIRED) URI authorizationUrl,
        @Schema(description = "인증 요청 유효기간(초)", requiredMode = Schema.RequiredMode.REQUIRED) long expiresInSeconds) {
    @Override public String toString() { return "SocialAuthStartResponse[redacted]"; }
}
