package com.daebangg.sayroam.auth.presentation.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SocialSignupRequest(
        @Schema(description = "결과 교환에서 받은 가입 티켓", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String signupTicket) {
    @Override public String toString() { return "SocialSignupRequest[redacted]"; }
}
