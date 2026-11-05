package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AccountVerificationConfirmRequest(
        @Schema(description = "발급 시와 같은 목적", example = "PASSWORD_CHANGE")
        @NotNull AccountVerificationPurpose purpose,
        @Schema(description = "발급 API가 반환한 verificationId") @NotNull UUID verificationId,
        @Schema(description = "메일로 받은 6자리 숫자 문자열", example = "012345")
        @NotNull @Pattern(regexp = "^[0-9]{6}$") String code) {
    @Override
    public String toString() {
        return "AccountVerificationConfirmRequest[redacted]";
    }
}
