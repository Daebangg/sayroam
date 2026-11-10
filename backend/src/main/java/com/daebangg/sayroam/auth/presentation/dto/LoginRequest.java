package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.global.validation.InputText;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.daebangg.sayroam.auth.domain.PasswordPolicy;

public record LoginRequest(
        @Schema(description = "가입한 이메일. 앞뒤 공백 제거 후 소문자로 정규화", example = "user@example.com")
        @NotBlank @Email @Size(max = 254)
        @Pattern(regexp = EmailAddress.PATTERN, message = "must be an ASCII email address") String email,
        @Schema(description = "가입한 비밀번호. 기존 계정의 8~128자 비밀번호도 공백·대소문자를 그대로 검증", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.LEGACY_MAX_LENGTH) String password,
        @Schema(description = "true는 영속 쿠키, false는 Max-Age 없는 브라우저 세션 쿠키. 생략·null은 기존 동작인 true. 갱신 시에도 선택을 유지", defaultValue = "true", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Boolean rememberMe) {
    public LoginRequest {
        email = InputText.trim(email);
        rememberMe = rememberMe == null || rememberMe;
    }

    @Override
    public String toString() {
        return "LoginRequest[redacted]";
    }
}
