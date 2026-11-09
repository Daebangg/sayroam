package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"resetProof", "expiresInSeconds"})
public record PasswordResetConfirmResponse(
        @Schema(description = "코드를 확인한 이메일의 비밀번호 재설정에만 한 번 사용할 증명. 로그인 토큰이 아님") String resetProof,
        @Schema(description = "재설정 증명의 남은 유효기간(초)") long expiresInSeconds) {
    @Override public String toString() { return "PasswordResetConfirmResponse[resetProof=<redacted>, expiresInSeconds=" + expiresInSeconds + "]"; }
}
