package com.daebangg.sayroam.auth.presentation.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;

@Tag(name = "Auth")
public interface AuthCsrfApi {

    @Operation(summary = "CSRF 쿠키 최초 발급", description = "응답의 XSRF-TOKEN 쿠키 값을 로그인·가입·토큰 갱신·로그아웃 등 이후 변경 요청의 X-XSRF-TOKEN 헤더에 전달합니다. 토큰은 응답 본문에 넣지 않습니다. 로그인·갱신·로그아웃은 현재 CSRF 쿠키를 유지하며 쿠키가 없거나 만료됐으면 다시 준비합니다.")
    @ApiResponse(responseCode = "204", description = "CSRF 쿠키 준비 완료 (Cache-Control: no-store)")
    ResponseEntity<Void> csrf(@Parameter(hidden = true) CsrfToken token);
}
