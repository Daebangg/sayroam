package com.daebangg.sayroam.auth.presentation;

import com.daebangg.sayroam.auth.application.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController {
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @Operation(summary = "이메일 인증 코드 발급·재발송", description = "CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 코드는 6자리이며 응답에 포함하지 않습니다. 인증 코드 확인 API는 후속 구현입니다.")
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
    @Parameter(name = "X-XSRF-TOKEN", in = ParameterIn.HEADER, required = true, description = "XSRF-TOKEN 쿠키에서 복사한 원문 토큰")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationService.IssuedRequest> request(
            @Valid @RequestBody EmailRequest body, HttpServletRequest request) {
        // Forwarded/X-Forwarded-For는 신뢰하지 않는다. 프록시 구성은 별도 신뢰 경계 설정이 필요하다.
        var issued = service.request(body.email(), request.getRemoteAddr());
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(issued);
    }

    public record EmailRequest(
            @Schema(description = "앞뒤 공백 제거 후 ASCII 이메일을 소문자로 정규화. 가입·로그인에도 같은 기준 적용 예정", example = "user@example.com")
            @NotBlank @Email @Size(max = 254) @Pattern(regexp = "^[!-~]+$", message = "must be an ASCII email address") String email) {
        public EmailRequest {
            email = email == null ? null : email.strip();
        }
    }
}
