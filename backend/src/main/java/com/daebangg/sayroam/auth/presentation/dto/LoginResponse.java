package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LoginResponse(
        @Schema(description = "API 요청의 Authorization 헤더로 전달하는 Access Token", requiredMode = Schema.RequiredMode.REQUIRED) String accessToken,
        @Schema(description = "인증 방식", allowableValues = "Bearer", requiredMode = Schema.RequiredMode.REQUIRED) String tokenType,
        @Schema(description = "토큰 유효기간(초)", example = "900", requiredMode = Schema.RequiredMode.REQUIRED) long expiresIn) {
    @Override
    public String toString() {
        return "LoginResponse[redacted]";
    }
}
