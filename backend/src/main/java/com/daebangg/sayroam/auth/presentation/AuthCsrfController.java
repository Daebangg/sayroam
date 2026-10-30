package com.daebangg.sayroam.auth.presentation;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthCsrfController {
    @Operation(summary = "CSRF 쿠키 최초 발급", description = "응답의 XSRF-TOKEN 쿠키 값을 이후 변경 요청의 X-XSRF-TOKEN 헤더에 전달합니다. 토큰은 응답 본문에 넣지 않습니다.")
    @ApiResponse(responseCode = "204", description = "CSRF 쿠키 준비 완료 (Cache-Control: no-store)")
    @GetMapping("/api/auth/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
