package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.LoginService;
import com.daebangg.sayroam.auth.presentation.dto.LoginRequest;
import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
public class LoginController {
    private final LoginService service;
    private final RefreshTokenCookie refreshCookie;

    public LoginController(LoginService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Operation(summary = "이메일·비밀번호 로그인", description = "XSRF-TOKEN 쿠키와 X-XSRF-TOKEN 헤더 필요. 활성·이메일 인증 회원의 비밀번호를 확인해 Access Token을 본문에, Refresh Token을 HttpOnly 쿠키에 발급합니다. Redis 저장이 완료된 뒤 응답합니다. 이메일 없음·비밀번호 불일치·탈퇴·비밀번호 미등록은 같은 실패 응답입니다. Authorization 헤더 없이 호출하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token과 Refresh 쿠키 발급. Cache-Control: no-store",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "sayroam_refresh: HttpOnly, SameSite=Strict, Path=/api/auth, 기본 Secure. local 프로필의 HTTP 개발 환경에서만 Secure 비활성화")),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/LoginUnauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true,
            description = "XSRF-TOKEN 쿠키에서 복사한 원문 토큰")
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        var token = service.login(request.email(), request.password());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(token.refreshToken(), token.refreshExpiresIn()).toString())
                .body(new LoginResponse(token.access().accessToken(), "Bearer", token.access().expiresIn()));
    }
}
