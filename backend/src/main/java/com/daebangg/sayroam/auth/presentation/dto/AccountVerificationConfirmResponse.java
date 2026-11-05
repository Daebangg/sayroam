package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.application.service.AccountVerificationService;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"accountProof", "purpose", "expiresInSeconds"})
public record AccountVerificationConfirmResponse(
        @Schema(description = "회원·현재 이메일·목적에 묶인 일회성 난수 증명. 가입용 증명·로그인 토큰이 아님")
        String accountProof,
        AccountVerificationPurpose purpose,
        @Schema(description = "증명 만료까지 남은 초") long expiresInSeconds) {
    public static AccountVerificationConfirmResponse from(AccountVerificationService.ConfirmedRequest confirmed) {
        return new AccountVerificationConfirmResponse(confirmed.accountProof(), confirmed.purpose(),
                confirmed.expiresInSeconds());
    }

    @Override
    public String toString() {
        return "AccountVerificationConfirmResponse[accountProof=<redacted>, purpose=" + purpose
                + ", expiresInSeconds=" + expiresInSeconds + "]";
    }
}
