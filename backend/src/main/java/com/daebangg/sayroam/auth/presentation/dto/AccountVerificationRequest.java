package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AccountVerificationRequest(
        @Schema(description = "본인 확인 목적. 회원·이메일은 인증된 계정에서 결정", example = "PASSWORD_CHANGE")
        @NotNull AccountVerificationPurpose purpose) {
}
