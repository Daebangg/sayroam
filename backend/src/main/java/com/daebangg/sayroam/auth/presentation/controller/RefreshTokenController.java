package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.service.RefreshTokenService;
import com.daebangg.sayroam.auth.presentation.api.RefreshTokenApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RefreshTokenController implements RefreshTokenApi {
    private final RefreshTokenService service;
    private final RefreshTokenCookie refreshCookie;

    public RefreshTokenController(RefreshTokenService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PostMapping(value = "/api/auth/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        TokenPairResult pair = service.refresh(refreshCookie.read(request));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(pair.refreshToken(), pair.refreshExpiresIn(), pair.rememberMe()).toString())
                .body(new LoginResponse(pair.access().accessToken(), "Bearer", pair.access().expiresIn()));
    }
}
