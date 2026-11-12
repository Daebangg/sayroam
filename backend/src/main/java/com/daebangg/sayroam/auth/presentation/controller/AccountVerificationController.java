package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.AccountVerificationService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.api.AccountVerificationApi;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationConfirmRequest;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationConfirmResponse;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationRequest;
import com.daebangg.sayroam.auth.presentation.dto.AccountVerificationResponse;
import com.daebangg.sayroam.global.mail.EmailLanguage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/account-verifications")
public class AccountVerificationController implements AccountVerificationApi {
    private final AccountVerificationService service;

    public AccountVerificationController(AccountVerificationService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountVerificationResponse> request(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountVerificationRequest body, HttpServletRequest request,
            @RequestHeader(value = HttpHeaders.ACCEPT_LANGUAGE, required = false) String acceptLanguage) {
        // 신뢰할 프록시 구성이 없으므로 Forwarded/X-Forwarded-For를 사용하지 않는다.
        AccountVerificationService.IssuedRequest issued = service.request(account.userId(), account.authVersion(), body.purpose(), request.getRemoteAddr(), EmailLanguage.fromAcceptLanguage(acceptLanguage));
        return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                .body(AccountVerificationResponse.from(issued));
    }

    @Override
    @PostMapping(path = "/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccountVerificationConfirmResponse> confirm(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountVerificationConfirmRequest body) {
        AccountVerificationService.ConfirmedRequest confirmed = service.confirm(account.userId(), account.authVersion(), body.purpose(),
                body.verificationId().toString(), body.code());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(AccountVerificationConfirmResponse.from(confirmed));
    }
}
