package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.LogoutService;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
public class LogoutController {
    private final LogoutService service;
    private final RefreshTokenCookie refreshCookie;

    public LogoutController(LogoutService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Operation(summary = "현재 로그인 로그아웃", description = "XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. sayroam_refresh 쿠키에 연결된 로그인만 갱신 불가로 만들고 쿠키를 만료시킵니다. Access Token은 필요하지 않으며 Authorization 헤더는 무시합니다. 쿠키 없음·형식 오류·만료·이미 폐기된 토큰도 204로 정리합니다. 같은 이름의 중복 쿠키는 401로 거절합니다. 이미 발급한 Access Token은 만료(기본 15분, 최대 30초 시간 오차)까지 유효하므로 클라이언트 메모리에서도 제거해야 합니다. 모든 기기 로그아웃·계정 삭제·비밀번호 변경은 포함하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "현재 로그인의 후속 갱신 차단 및 Refresh 쿠키 만료. 본문 없음. 쿠키가 없거나 유효하지 않으면 브라우저 정리만 수행. Cache-Control: no-store",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "sayroam_refresh=; Path=/api/auth; Max-Age=0; HttpOnly; SameSite=Strict. 발급과 같은 Secure 설정")),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/LogoutCookieInvalid"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/LogoutUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @Parameter(name = RefreshTokenCookie.NAME, in = ParameterIn.COOKIE, required = false,
            description = "브라우저가 자동 전송하는 HttpOnly 쿠키. 없는 경우에도 CSRF 검증 후 204로 브라우저 정리")
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        service.logout(refreshCookie.read(request));
        // 저장소 오류 시 성공이나 쿠키 삭제로 덮지 않아 폐기 재시도에 필요한 쿠키를 유지한다.
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
