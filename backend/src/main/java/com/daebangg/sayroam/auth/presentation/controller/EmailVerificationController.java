package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.EmailVerificationService;
import com.daebangg.sayroam.auth.presentation.dto.EmailRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Email Verification")
@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @Operation(summary = "이메일 인증 코드 발급·재발송", description = "CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 코드는 6자리이며 응답에 포함하지 않습니다. 재발급 승인 시 이전 코드와 가입용 증명은 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailRequestSuperseded"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailRateLimited"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/EmailDeliveryFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/EmailVerificationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationResponse> request(
            @Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        // Forwarded/X-Forwarded-For는 신뢰하지 않는다. 프록시 구성은 별도 신뢰 경계 설정이 필요하다.
        var issued = service.request(body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(EmailVerificationResponse.from(issued));
    }

    @Operation(summary = "이메일 인증 코드 검증 및 가입용 증명 발급", description = "이메일·요청 ID·코드를 원자적으로 확인하고 코드를 소비합니다. 성공한 코드는 다시 사용할 수 없습니다. SMTP 설정 없이 기존 코드 검증은 가능하지만 Redis는 필요합니다. 가입용 증명은 POST /api/auth/signup에서 한 번 사용할 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이메일에 바인딩된 가입용 증명 발급. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/EmailCodeInvalid"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailVerificationConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/EmailCodeExpired"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailCodeAttemptsExceeded"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/EmailConfirmationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationConfirmResponse> confirm(
            @Valid @RequestBody EmailVerificationConfirmRequest body) {
        var confirmed = service.confirm(body.email(), body.verificationId().toString(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(EmailVerificationConfirmResponse.from(confirmed));
    }
}
