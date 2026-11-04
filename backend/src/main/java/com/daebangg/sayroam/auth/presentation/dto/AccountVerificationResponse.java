package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.application.service.AccountVerificationService;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;

public record AccountVerificationResponse(
        @Schema(description = "확인 요청을 식별하는 UUID") String verificationId,
        AccountVerificationPurpose purpose,
        @Schema(description = "코드 만료까지 남은 초") long expiresInSeconds,
        @Schema(description = "같은 회원이 어느 목적이든 다시 발송하기 전 대기 초") long resendAfterSeconds) {
    public static AccountVerificationResponse from(AccountVerificationService.IssuedRequest issued) {
        return new AccountVerificationResponse(issued.verificationId(), issued.purpose(),
                issued.expiresInSeconds(), issued.resendAfterSeconds());
    }
}
