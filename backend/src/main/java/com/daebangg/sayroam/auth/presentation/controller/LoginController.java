package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.LoginService;
import com.daebangg.sayroam.auth.presentation.dto.LoginRequest;
import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
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

    @Operation(summary = "이메일·비밀번호 로그인", description = "XSRF-TOKEN 쿠키와 X-XSRF-TOKEN 헤더 필요. 활성·이메일 인증 회원의 비밀번호를 확인해 Access Token을 본문에, Refresh Token을 HttpOnly 쿠키에 발급합니다. Redis 저장이 완료된 뒤 응답합니다. 이메일 없음·비밀번호 불일치·탈퇴·비밀번호 미등록은 같은 실패 응답입니다. 첫 실패부터 300초 동안 정규화한 이메일은 5회, 서버가 확인한 IP는 여러 이메일에 걸쳐 100회 실패하면 각각 300초 차단합니다. 이메일 다섯 번째 실패 또는 IP 백 번째 실패부터 429이며 차단 중 요청으로 연장하지 않습니다. 401의 remainingAttempts는 해당 이메일 기준이며 로그인 성공 시 이메일 실패 횟수만 초기화합니다. rememberMe=false는 세션 쿠키, 생략·true는 영속 쿠키이며 갱신에도 유지합니다. Authorization 헤더 없이 호출하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Access Token과 Refresh 쿠키 발급. Cache-Control: no-store",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "sayroam_refresh: HttpOnly, SameSite=Strict, Path=/api/auth, 기본 Secure. local 프로필의 HTTP 개발 환경에서만 Secure 비활성화")),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/LoginUnauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/LoginRateLimited"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/LoginUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(value = "/api/auth/login", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        // 신뢰한 프록시 구성이 없으므로 클라이언트가 보낸 Forwarded 계열 헤더는 사용하지 않는다.
        var token = service.login(request.email(), request.password(), servletRequest.getRemoteAddr(), request.rememberMe());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(token.refreshToken(), token.refreshExpiresIn(), token.rememberMe()).toString())
                .body(new LoginResponse(token.access().accessToken(), "Bearer", token.access().expiresIn()));
    }
}
