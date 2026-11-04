package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Schema(description = "가입한 이메일. 앞뒤 공백 제거 후 소문자로 정규화", example = "user@example.com")
        @NotBlank @Email @Size(max = 254)
        @Pattern(regexp = "^[!-~]+$", message = "must be an ASCII email address") String email,
        @Schema(description = "가입한 비밀번호. 공백·대소문자를 그대로 검증", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 128) String password) {
    public LoginRequest {
        email = email == null ? null : email.strip();
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
