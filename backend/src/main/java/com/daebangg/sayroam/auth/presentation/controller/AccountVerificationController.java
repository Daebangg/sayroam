package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.AccountVerificationService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationRequest;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account Verification")
@RestController
@RequestMapping("/api/auth/account-verifications")
@SecurityRequirement(name = "bearerAuth")
public class AccountVerificationController {
    private final AccountVerificationService service;

    public AccountVerificationController(AccountVerificationService service) {
        this.service = service;
    }

    @Operation(summary = "계정 관리용 이메일 본인 확인 코드 발급·재발송", description = "Bearer 인증과 CSRF 쿠키·X-XSRF-TOKEN 헤더가 모두 필요합니다. DB의 현재 인증된 대표 이메일로만 발송합니다. PASSWORD_CHANGE/ACCOUNT_DELETION의 코드·증명은 분리하고 발송 제한은 합산합니다. 승인된 재발급은 같은 목적의 이전 코드·증명을 무효화합니다. 이 API는 비밀번호 변경·탈퇴를 실행하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccountAuthenticationRevoked"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccountVerificationForbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailRequestSuperseded"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailRateLimited"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/EmailDeliveryFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AccountVerificationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountVerificationResponse> request(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountVerificationRequest body, HttpServletRequest request) {
        // 신뢰할 프록시 구성이 없으므로 Forwarded/X-Forwarded-For를 사용하지 않는다.
        var issued = service.request(account.userId(), account.authVersion(), body.purpose(), request.getRemoteAddr());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(AccountVerificationResponse.from(issued));
    }

    @Operation(summary = "계정 관리용 코드 검증·일회성 증명 발급", description = "같은 회원·현재 이메일·목적·인증 버전·요청 ID·코드를 확인한 후 코드를 소비하고 accountProof를 한 번 반환합니다. 증명은 해당 목적의 작업에서 한 번만 소비할 수 있습니다. 가입용 signupProof와 호환되지 않습니다. 확인 응답을 잃으면 코드를 재사용하지 말고 새로 발급해야 합니다. PASSWORD_CHANGE 증명은 PATCH /api/auth/password, ACCOUNT_DELETION 증명은 POST /api/auth/account-deletion에서 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목적별 일회성 accountProof. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/EmailCodeInvalid"),
            @ApiResponse(responseCode = "401", ref = "#/components/responses/AccountAuthenticationRevoked"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/AccountVerificationForbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailVerificationConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/EmailCodeExpired"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailCodeAttemptsExceeded"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/AccountConfirmationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountVerificationConfirmResponse> confirm(
            @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountVerificationConfirmRequest body) {
        var confirmed = service.confirm(account.userId(), account.authVersion(), body.purpose(),
                body.verificationId().toString(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(AccountVerificationConfirmResponse.from(confirmed));
    }
}
