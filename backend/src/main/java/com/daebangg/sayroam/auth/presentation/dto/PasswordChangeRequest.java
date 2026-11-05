package com.daebangg.sayroam.auth.presentation.dto;

import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @Schema(description = "PASSWORD_CHANGE 본인 확인에서 발급한 일회성 증명", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String accountProof,
        @Schema(description = "가입과 같은 8~128자 정책. 공백 제거·대소문자 변환 없이 해시 처리", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) String newPassword) {
    @Override
    public String toString() {
        return "PasswordChangeRequest[redacted]";
    }
}
