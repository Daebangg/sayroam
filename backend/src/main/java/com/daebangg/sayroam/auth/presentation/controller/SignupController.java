package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.SignupService;
import com.daebangg.sayroam.auth.presentation.dto.SignupRequest;
import com.daebangg.sayroam.auth.presentation.dto.SignupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth/signup")
public class SignupController {
    private final SignupService service;

    public SignupController(SignupService service) {
        this.service = service;
    }

    @Operation(summary = "이메일 인증 후 회원가입", description = "CSRF 쿠키와 X-XSRF-TOKEN 헤더 필요. 가입용 증명을 먼저 일회성 소비한 뒤 일반 회원과 비밀번호 해시를 함께 저장합니다. DB 실패 시 증명을 복구하지 않으며 새 이메일 인증이 필요합니다. 자동 로그인과 JWT 발급은 수행하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원 생성 및 DB 커밋 완료. Cache-Control: no-store"),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/SignupBadRequest"),
            @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/SignupConflict"),
            @ApiResponse(responseCode = "410", ref = "#/components/responses/SignupProofExpired"),
            @ApiResponse(responseCode = "503", ref = "#/components/responses/SignupUnavailable"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest body) {
        long userId = service.signup(body.email(), body.password(), body.nickname(), body.signupProof());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(new SignupResponse(userId));
    }
}
