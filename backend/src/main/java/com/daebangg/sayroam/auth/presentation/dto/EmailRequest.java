package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.global.validation.InputText;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @Schema(description = "앞뒤 공백 제거 후 ASCII 이메일을 소문자로 정규화. 로컬 부분 최대 64자·도메인 라벨 최대 63자, 전체 최대 254자", example = "user@example.com")
        @NotBlank @Email @Size(max = 254) @Pattern(regexp = EmailAddress.PATTERN, message = "must be an ASCII email address") String email) {
    public EmailRequest {
        email = InputText.trim(email);
    }
}
