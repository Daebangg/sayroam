package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.EmailVerificationService;
import com.daebangg.sayroam.auth.presentation.api.EmailVerificationApi;
import com.daebangg.sayroam.auth.presentation.dto.EmailRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.EmailVerificationResponse;
import com.daebangg.sayroam.global.mail.EmailLanguage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
public class EmailVerificationController implements EmailVerificationApi {
    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationResponse> request(
            @Valid @RequestBody EmailRequest body, HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        // Forwarded/X-Forwarded-For는 신뢰하지 않는다. 프록시 구성은 별도 신뢰 경계 설정이 필요하다.
        EmailVerificationService.IssuedRequest issued = service.request(body.email(), request.getRemoteAddr(), EmailLanguage.fromAcceptLanguage(acceptLanguage));
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(EmailVerificationResponse.from(issued));
    }

    @Override
    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationConfirmResponse> confirm(
            @Valid @RequestBody EmailVerificationConfirmRequest body) {
        EmailVerificationService.ConfirmedRequest confirmed = service.confirm(body.email(), body.verificationId().toString(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(EmailVerificationConfirmResponse.from(confirmed));
    }
}
