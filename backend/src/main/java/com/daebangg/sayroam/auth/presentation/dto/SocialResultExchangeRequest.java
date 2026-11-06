package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SocialResultExchangeRequest(
        @Schema(description = "콜백 후 화면 fragment의 social_result 값. 일회성·브라우저 바인딩", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{43}") String resultCode) {
    @Override public String toString() { return "SocialResultExchangeRequest[redacted]"; }
}
