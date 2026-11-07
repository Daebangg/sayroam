package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @Schema(description = "코드를 확인한 이메일", example = "user@example.com")
        @NotBlank @Email @Size(max = 254) @Pattern(regexp = "^[!-~]+$") String email,
        @Schema(description = "비밀번호 재설정 코드 확인에서 발급한 전용 일회성 증명", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String resetProof,
        @Schema(description = "가입과 같은 8~128자 정책. 공백 제거·대소문자 변환 없이 처리", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword) {
    public PasswordResetRequest { email = email == null ? null : email.strip(); }
    @Override public String toString() { return "PasswordResetRequest[redacted]"; }
}
