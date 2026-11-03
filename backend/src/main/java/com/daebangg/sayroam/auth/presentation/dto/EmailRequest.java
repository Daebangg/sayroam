package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @Schema(description = "앞뒤 공백 제거 후 ASCII 이메일을 소문자로 정규화. 가입·로그인에도 같은 기준 적용 예정", example = "user@example.com")
        @NotBlank @Email @Size(max = 254) @Pattern(regexp = "^[!-~]+$", message = "must be an ASCII email address") String email) {
    public EmailRequest {
        email = email == null ? null : email.strip();
    }
}
