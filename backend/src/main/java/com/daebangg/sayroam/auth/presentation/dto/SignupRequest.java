package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.Password;
import com.daebangg.sayroam.global.validation.InputText;
import com.daebangg.sayroam.user.domain.Nickname;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "인증한 이메일. 공백 제거 후 소문자로 정규화", example = "user@example.com")
        @NotBlank @Email @Size(min = 1, max = 254)
        @Pattern(regexp = EmailAddress.PATTERN, message = "must be an ASCII email address") String email,
        @Schema(description = "8~24자, 대문자·숫자·특수문자 포함, 공백·숨김 문자 불가. 공백 제거·대소문자 변환 없이 해시 처리", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = Password.MIN_LENGTH, max = Password.MAX_LENGTH)
        @Pattern(regexp = Password.PATTERN) String password,
        @Schema(description = "앞뒤 공백 제거 후 1~50자. 제어·숨김 문자 불가", example = "여행자")
        @NotBlank @Size(min = 1, max = 50)
        @Pattern(regexp = Nickname.PATTERN, message = "must not contain control characters") String nickname,
        @Schema(description = "코드 검증 API에서 실제로 받은 43자 가입용 증명", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = 43, max = 43)
        @Pattern(regexp = "^[A-Za-z0-9_-]{43}$", message = "must be a 43-character Base64URL token")
        String signupProof) {
    public SignupRequest {
        email = InputText.trim(email);
        nickname = InputText.trim(nickname);
    }

    @Override
    public String toString() {
        return "SignupRequest[redacted]";
    }
}
