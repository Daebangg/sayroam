package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.PasswordChangeService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.PasswordChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account Management")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class PasswordChangeController {
    private final PasswordChangeService service;
    private final RefreshTokenCookie refreshCookie;

    public PasswordChangeController(PasswordChangeService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Operation(summary = "본인 비밀번호 변경·모든 기존 로그인 무효화", description = "Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. PASSWORD_CHANGE 이메일 본인 확인의 accountProof를 한 번 소비합니다. 기존 비밀번호가 등록된 본인 계정만 변경하며 소셜 전용 계정에 비밀번호를 새로 등록하지 않습니다. 해시·변경 시각·인증 버전 증가를 같은 DB 트랜잭션으로 커밋합니다. 모든 기기의 기존 Access/Refresh는 이후 인증·갱신에서 거절됩니다. 성공 시 현재 Refresh 쿠키를 만료시키며 프론트는 Access Token·회원 정보·진행 중 갱신을 정리하고 새 비밀번호로 재로그인해야 합니다. 응답 유실 시 자동 재전송하지 마세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "비밀번호 변경과 기존 로그인 무효화 완료. Cache-Control: no-store. 재로그인 필요",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "발급과 동일한 속성으로 sayroam_refresh 만료. XSRF-TOKEN은 유지")),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/PasswordChangeBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccountAuthenticationRevoked"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/PasswordChangeForbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/PasswordChangeConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/AccountProofExpired"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PasswordChangeUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PatchMapping(value = "/api/auth/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> change(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody PasswordChangeRequest body) {
        service.change(account.userId(), account.authVersion(), body.accountProof(), body.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
