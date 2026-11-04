package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.AccountDeletionService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.AccountDeletionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account management")
@SecurityRequirement(name = "bearerAuth")
@RestController
public class AccountDeletionController {
    private final AccountDeletionService service;
    private final RefreshTokenCookie refreshCookie;

    public AccountDeletionController(AccountDeletionService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Operation(summary = "본인 회원 탈퇴·계정 프로필 및 로그인 수단 정리", description = "Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더 필요. ACCOUNT_DELETION 이메일 본인 확인의 accountProof만 한 번 소비합니다. 현재 회원의 탈퇴 상태·프로필 익명화·인증 버전 증가와 비밀번호·소셜 로그인 연결·개인 여행 성향·수신 알림 삭제를 한 DB 트랜잭션으로 커밋합니다. 회원 ID와 채팅·정산 등 공유 기록의 참조는 보존합니다. 모든 기기의 기존 로그인·Access 인증·Refresh 갱신을 차단하고 현재 Refresh 쿠키를 만료시킵니다. 같은 이메일로 재가입하면 새 회원이며 기존 계정을 복원하지 않습니다. 응답 유실·DB 커밋 결과 불확실 시 자동 재전송·자동 재가입하지 마세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "탈퇴 및 계정 정리 완료. Cache-Control: no-store. 본문·새 토큰 없음",
                    headers = @Header(name = "Set-Cookie", schema = @Schema(type = "string"), description = "발급과 같은 속성으로 sayroam_refresh 만료. XSRF-TOKEN은 유지")),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/AccountDeletionBadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccountAuthenticationRevoked"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccountVerificationForbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/AccountDeletionConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/AccountProofExpired"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AccountDeletionUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "XSRF-TOKEN 쿠키의 원문")
    @PostMapping(value = "/api/auth/account-deletion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountDeletionRequest body) {
        service.delete(account.userId(), account.authVersion(), body.accountProof());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
