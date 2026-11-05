package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.application.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.media.Schema;

// DTO 분리 이후에도 기존 OpenAPI 스키마 이름을 유지한다.
@Schema(name = "IssuedRequest", requiredProperties = {"verificationId", "expiresInSeconds", "resendAfterSeconds"})
public record EmailVerificationResponse(String verificationId, long expiresInSeconds, long resendAfterSeconds) {
    public static EmailVerificationResponse from(EmailVerificationService.IssuedRequest issued) {
        return new EmailVerificationResponse(issued.verificationId(), issued.expiresInSeconds(), issued.resendAfterSeconds());
    }
}
