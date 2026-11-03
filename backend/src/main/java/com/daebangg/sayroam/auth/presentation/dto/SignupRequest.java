package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "인증한 이메일. 공백 제거 후 소문자로 정규화", example = "user@example.com")
        @NotBlank @Email @Size(min = 1, max = 254)
        @Pattern(regexp = "^[!-~]+$", message = "must be an ASCII email address") String email,
        @Schema(description = "8~128자. 공백 제거·대소문자 변환 없이 해시 처리", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 8, max = 128) String password,
        @Schema(description = "앞뒤 공백 제거 후 1~50자. 제어 문자 불가", example = "여행자")
        @NotBlank @Size(min = 1, max = 50)
        @Pattern(regexp = "^[^\\x00-\\x1F\\x7F]+$", message = "must not contain control characters") String nickname,
        @Schema(description = "코드 검증 API에서 실제로 받은 43자 가입용 증명", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 43, max = 43)
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$", message = "must be a 43-character Base64URL token")
        String signupProof) {
    public SignupRequest {
        email = email == null ? null : email.strip();
        nickname = nickname == null ? null : nickname.strip();
    }

    @Override
    public String toString() {
        return "SignupRequest[redacted]";
    }
}
