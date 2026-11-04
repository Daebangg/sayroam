package com.daebangg.sayroam.user.presentation.controller;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.user.presentation.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users")
@RestController
public class CurrentUserController {
    @Operation(summary = "내 회원 정보 조회", description = "Authorization: Bearer <accessToken> 필요. 토큰 검증 후 DB에서 조회한 현재 회원 정보를 반환합니다. 탈퇴·삭제 회원은 인증되지 않으며 요청에서 다른 회원의 ID를 받지 않습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "현재 회원 정보. Cache-Control: no-store"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AuthenticationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @GetMapping(value = "/api/users/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CurrentUserResponse> me(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CurrentUserResponse(account.userId(), account.email(), account.nickname(),
                        account.profileImageUrl(), account.role()));
    }
}
