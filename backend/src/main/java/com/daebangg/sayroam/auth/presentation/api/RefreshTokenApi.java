package com.daebangg.sayroam.auth.presentation.api;

import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Auth")
public interface RefreshTokenApi {

    @Operation(summary = "토큰 갱신", description = "브라우저의 sayroam_refresh HttpOnly 쿠키와 XSRF-TOKEN 쿠키·X-XSRF-TOKEN 헤더가 필요합니다. Access Token 없이 호출하며 이 경로에서는 Authorization 헤더를 인증에 사용하지 않습니다. 활성 회원을 확인한 뒤 Refresh Token을 일회성 교체합니다. 이전 값 재사용·동시 갱신은 해당 로그인의 갱신 권한을 폐기합니다. 클라이언트는 갱신 요청을 직렬화하고 불확실한 응답을 자동 재시도하지 않아야 합니다. 절대 만료는 연장하지 않으며 로그인 시 rememberMe의 세션/영속 쿠키 선택을 유지합니다.")
    @SecurityRequirement(name = "refreshCookie")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "새 Access Token과 교체된 Refresh 쿠키. Cache-Control: no-store",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "sayroam_refresh 교체. 영속 쿠키는 남은 절대 유효기간만 Max-Age로 지정, 세션 쿠키는 Max-Age 생략")),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/RefreshUnauthorized"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<LoginResponse> refresh(HttpServletRequest request);
}
