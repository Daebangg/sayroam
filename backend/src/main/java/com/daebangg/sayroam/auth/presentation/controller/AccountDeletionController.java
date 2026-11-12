package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.AccountDeletionService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.api.AccountDeletionApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.AccountDeletionRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountDeletionController implements AccountDeletionApi {
    private final AccountDeletionService service;
    private final RefreshTokenCookie refreshCookie;

    public AccountDeletionController(AccountDeletionService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PostMapping(value = "/api/auth/account-deletion", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody AccountDeletionRequest body) {
        service.delete(account.userId(), account.authVersion(), body.accountProof());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
