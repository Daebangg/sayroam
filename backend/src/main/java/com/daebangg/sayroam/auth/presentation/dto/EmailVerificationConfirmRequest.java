package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.global.validation.InputText;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EmailVerificationConfirmRequest(
        @Schema(description = "발급 시와 같은 이메일. 공백 제거 후 소문자로 정규화", example = "user@example.com")
        @NotBlank @Email @Size(max = 254) @Pattern(regexp = EmailAddress.PATTERN, message = "must be an ASCII email address") String email,
        @Schema(description = "발급 API가 반환한 verificationId") @NotNull UUID verificationId,
        @Schema(description = "메일로 받은 6자리 숫자. 앞자리 0을 보존하는 문자열", example = "012345")
        @NotNull @Pattern(regexp = "^[0-9]{6}$") String code) {
    public EmailVerificationConfirmRequest {
        email = InputText.trim(email);
    }

    @Override
    public String toString() {
        return "EmailVerificationConfirmRequest[redacted]";
    }
}
