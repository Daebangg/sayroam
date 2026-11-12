package com.daebangg.sayroam.auth.presentation.api;

import com.daebangg.sayroam.auth.presentation.dto.EmailRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationResponse;
import com.daebangg.sayroam.auth.presentation.dto.PasswordResetConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.PasswordResetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

@Tag(name = "Password Reset")
public interface PasswordResetApi {

    @Operation(operationId = "requestPasswordReset", summary = "비밀번호 찾기 코드 발송·재발송",
            description = "로그인 없이 CSRF 쿠키·X-XSRF-TOKEN 헤더로 요청합니다. Bearer 헤더 없이 호출하세요. 가입·비밀번호 등록 여부와 무관하게 동일한 안내 메일·6자리 코드와 응답을 제공합니다. 코드 확인은 이메일 소유 확인이며 비밀번호 재설정 가능 여부를 보장하지 않습니다. 이메일/IP 발송 제한과 재발송 대기는 계정 여부와 무관하게 적용합니다. 재발급 승인 시 기존 코드·재설정 증명을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "안내 메일 발송 요청 완료. 가입 여부·소셜 전용 여부·메일 도착은 보장하지 않음. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailRequestSuperseded"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailRateLimited"),
            @ApiResponse(responseCode = "502", ref = "#/components/responses/EmailDeliveryFailed"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PasswordResetVerificationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<EmailVerificationResponse> request(EmailRequest body, HttpServletRequest request,
            @Parameter(description = "인증 메일 언어. ko/en을 지역 태그와 q 우선순위로 선택하며 누락·미지원·잘못된 값은 한국어로 처리합니다.", example = "en-US,en;q=0.9,ko;q=0.8")
            String acceptLanguage);

    @Operation(operationId = "confirmPasswordReset", summary = "비밀번호 찾기 코드 확인·재설정 증명 발급",
            description = "로그인 없이 CSRF 쿠키·헤더로 이메일·verificationId·6자리 코드를 전달합니다. 성공한 코드는 일회성 소비합니다. resetProof는 재설정 전용이며 가입·로그인 중 변경·탈퇴 증명과 호환되지 않습니다. 발송 이후 계정이나 인증 버전이 바뀌면 새 코드가 필요합니다. 증명은 자동 저장하거나 요청을 자동 반복하지 마세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재설정 전용 증명 발급. 비밀번호 로그인 계정의 재설정에만 사용 가능. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/EmailCodeInvalid"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/EmailVerificationConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/EmailCodeExpired"),
            @ApiResponse(responseCode = "429", ref = "#/components/responses/EmailCodeAttemptsExceeded"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PasswordResetVerificationUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<PasswordResetConfirmResponse> confirm(EmailVerificationConfirmRequest body);

    @Operation(operationId = "resetPassword", summary = "비밀번호 재설정·모든 기존 로그인 무효화",
            description = "로그인 없이 CSRF 쿠키·헤더와 email·resetProof·newPassword를 전달합니다. 활성·이메일 인증·기존 비밀번호 로그인 계정에만 적용하며 소셜 전용 계정에 비밀번호를 추가하지 않습니다. 증명을 일회성 소비한 뒤 비밀번호 해시·변경 시각·authVersion을 같은 DB 트랜잭션에서 갱신합니다. 성공 시 자동 로그인하지 않으며 모든 기기의 기존 Access/Refresh를 차단합니다. 현재 브라우저의 로그인 상태·회원 정보·진행 중 갱신을 정리하고 새 비밀번호로 재로그인하세요. 응답 유실·503 시 자동 재전송하지 말고 재로그인으로 결과를 확인한 뒤 필요하면 새 코드를 요청하세요.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "재설정 완료. Cache-Control: no-store. 새 비밀번호로 재로그인 필요",
                    headers = @Header(name = "Set-Cookie", description = "현재 브라우저의 sayroam_refresh 쿠키 만료", schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/PasswordResetBadRequest"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/PasswordResetConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/PasswordResetProofExpired"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/PasswordResetUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    ResponseEntity<Void> reset(PasswordResetRequest body);
}
