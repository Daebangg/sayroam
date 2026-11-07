package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

public record SocialExchangeResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Status status,
        @Schema(description = "AUTHENTICATED일 때만 존재. Refresh는 HttpOnly 쿠키") @Nullable LoginResponse login,
        @Schema(description = "SIGNUP_REQUIRED일 때만 존재. 가입 전 일반 서비스 토큰은 발급하지 않음") @Nullable Signup signup) {
    public enum Status { AUTHENTICATED, SIGNUP_REQUIRED }

    public record Signup(
            @Schema(description = "가입 API에 한 번 전달할 브라우저 바인딩 티켓", requiredMode = Schema.RequiredMode.REQUIRED) String signupTicket,
            @Schema(description = "가입 티켓 유효기간(초)", requiredMode = Schema.RequiredMode.REQUIRED) long expiresInSeconds,
            @Schema(description = "true이면 #28 이메일 인증 후 signupProof 필요", requiredMode = Schema.RequiredMode.REQUIRED) boolean emailVerificationRequired,
            @Schema(description = "공급자가 제공한 입력 참고용 이메일. 검증 완료를 의미하지 않음", nullable = true) String suggestedEmail) {
        @Override public String toString() { return "SocialSignupRequired[redacted]"; }
    }
    @Override public String toString() { return "SocialExchangeResponse[redacted]"; }
}
