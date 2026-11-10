package com.daebangg.sayroam.auth.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record SocialAuthStartRequest(
        @Schema(description = "서버 허용 목록의 로컬 복귀 경로. 생략하면 /", defaultValue = "/")
        @Size(max = 200) String returnPath,
        @Schema(description = "이메일 미제공 안내 후 재시도할 때 true. Naver 정보 제공 재동의 화면 요청", defaultValue = "false")
        Boolean requestEmailConsent) {
    public SocialAuthStartRequest { returnPath = returnPath == null ? "/" : returnPath; }
}
