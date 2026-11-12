package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.RefreshTokenService;
import com.daebangg.sayroam.auth.presentation.api.LogoutApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogoutController implements LogoutApi {
    private final RefreshTokenService service;
    private final RefreshTokenCookie refreshCookie;

    public LogoutController(RefreshTokenService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        service.logout(refreshCookie.read(request));
        // 저장소 오류 시 성공이나 쿠키 삭제로 덮지 않아 폐기 재시도에 필요한 쿠키를 유지한다.
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
