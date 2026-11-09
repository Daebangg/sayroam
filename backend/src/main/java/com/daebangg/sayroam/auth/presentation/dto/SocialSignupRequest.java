package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.global.validation.InputText;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.daebangg.sayroam.user.domain.policy.UserProfilePolicy;
import jakarta.validation.constraints.Size;

public record SocialSignupRequest(
        @Schema(description = "결과 교환에서 받은 가입 티켓", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String signupTicket,
        @Schema(description = "앞뒤 공백 제거 후 1~50자", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 50) @Pattern(regexp = UserProfilePolicy.NICKNAME_PATTERN) String nickname,
        @Schema(description = "이메일 보완 시 인증한 이메일. 신뢰한 공급자 이메일이 있으면 생략하거나 동일한 값만 전달", nullable = true)
        @Email @Size(min = 1, max = 254) @Pattern(regexp = EmailAddress.PATTERN) String email,
        @Schema(description = "이메일 보완 시 #28에서 받은 가입 증명", accessMode = Schema.AccessMode.WRITE_ONLY, nullable = true)
        @Pattern(regexp = "[A-Za-z0-9_-]{43}") String signupProof) {
    public SocialSignupRequest {
        nickname = InputText.trim(nickname);
        email = InputText.trim(email);
    }
    @Override public String toString() { return "SocialSignupRequest[redacted]"; }
}
