package com.daebangg.sayroam.auth.presentation.api;

import com.daebangg.sayroam.auth.presentation.dto.EmailRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Email Verification")
public interface EmailVerificationApi {

    @Operation(summary = "이메일 인증 코드 발급·재발송", description = "CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 코드는 6자리이며 응답에 포함하지 않습니다. 발송 전 DB 가입 여부를 확인하며 이미 가입된 이메일은 메일을 보내지 않고 409 EMAIL_ALREADY_REGISTERED를 반환합니다. 재발급 승인 시 이전 코드와 가입용 증명은 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "SMTP 서버에 발송 요청 완료. 수신함 도착을 보장하지 않음"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/SignupEmailConflict"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailRateLimited"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/EmailDeliveryFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/EmailVerificationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<EmailVerificationResponse> request(
            EmailRequest body, HttpServletRequest request,
            @Parameter(description = "인증 메일 언어. ko/en을 지역 태그와 q 우선순위로 선택하며 누락·미지원·잘못된 값은 한국어로 처리합니다.", example = "en-US,en;q=0.9,ko;q=0.8")
            String acceptLanguage);

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
    ResponseEntity<EmailVerificationConfirmResponse> confirm(
            EmailVerificationConfirmRequest body);
}
