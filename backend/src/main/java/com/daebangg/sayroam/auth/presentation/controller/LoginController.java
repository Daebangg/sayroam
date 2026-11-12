package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.service.LoginService;
import com.daebangg.sayroam.auth.presentation.api.LoginApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.LoginRequest;
import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController implements LoginApi {
    private final LoginService service;
    private final RefreshTokenCookie refreshCookie;

    public LoginController(LoginService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        // 신뢰한 프록시 구성이 없으므로 클라이언트가 보낸 Forwarded 계열 헤더는 사용하지 않는다.
        TokenPairResult token = service.login(request.email(), request.password(), servletRequest.getRemoteAddr(), request.rememberMe());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(token.refreshToken(), token.refreshExpiresIn(), token.rememberMe()).toString())
                .body(new LoginResponse(token.access().accessToken(), "Bearer", token.access().expiresIn()));
    }
}
