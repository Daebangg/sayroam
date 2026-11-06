package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record SocialAuthStartRequest(
        @Schema(description = "서버 허용 목록의 로컬 복귀 경로. 생략하면 /", defaultValue = "/")
        @Size(max = 200) String returnPath) {
    public SocialAuthStartRequest { returnPath = returnPath == null ? "/" : returnPath; }
}
