package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.application.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.media.Schema;

public record EmailVerificationConfirmResponse(
        @Schema(description = "이 이메일의 가입에만 한 번 사용할 난수 증명. JWT·로그인 토큰이 아님") String signupProof,
        @Schema(description = "가입용 증명이 만료될 때까지 남은 초") long expiresInSeconds) {
    public static EmailVerificationConfirmResponse from(EmailVerificationService.ConfirmedRequest confirmed) {
        return new EmailVerificationConfirmResponse(confirmed.signupProof(), confirmed.expiresInSeconds());
    }

    @Override
    public String toString() {
        return "EmailVerificationConfirmResponse[signupProof=<redacted>, expiresInSeconds=" + expiresInSeconds + "]";
    }
}
