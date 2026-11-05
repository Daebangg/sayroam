package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AccountDeletionRequest(
        @Schema(description = "ACCOUNT_DELETION 본인 확인에서 발급한 일회성 증명", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9_-]{43}$") String accountProof) {
    @Override
    public String toString() {
        return "AccountDeletionRequest[redacted]";
    }
}
