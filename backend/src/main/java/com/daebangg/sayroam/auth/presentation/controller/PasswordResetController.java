package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.PasswordResetService;
import com.daebangg.sayroam.auth.presentation.api.PasswordResetApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.*;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordResetController implements PasswordResetApi {
    private final PasswordResetService service;
    private final RefreshTokenCookie refreshCookie;

    public PasswordResetController(PasswordResetService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PostMapping(value = "/api/auth/password-reset-verifications", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EmailVerificationResponse> request(@Valid @RequestBody EmailRequest body, HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        PasswordResetService.IssuedRequest issued = service.request(body.email(), request.getRemoteAddr(), EmailLanguage.fromAcceptLanguage(acceptLanguage));
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(new EmailVerificationResponse(issued.verificationId(), issued.expiresInSeconds(), issued.resendAfterSeconds()));
    }

    @Override
    @PostMapping(value = "/api/auth/password-reset-verifications/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PasswordResetConfirmResponse> confirm(@Valid @RequestBody EmailVerificationConfirmRequest body) {
        PasswordResetService.ConfirmedRequest confirmed = service.confirm(body.email(), body.verificationId().toString(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new PasswordResetConfirmResponse(confirmed.resetProof(), confirmed.expiresInSeconds()));
    }

    @Override
    @PostMapping(value = "/api/auth/password-reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reset(@Valid @RequestBody PasswordResetRequest body) {
        service.reset(body.email(), body.resetProof(), body.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
