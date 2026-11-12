package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.PasswordChangeService;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.presentation.api.PasswordChangeApi;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.PasswordChangeRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PasswordChangeController implements PasswordChangeApi {
    private final PasswordChangeService service;
    private final RefreshTokenCookie refreshCookie;

    public PasswordChangeController(PasswordChangeService service, RefreshTokenCookie refreshCookie) {
        this.service = service;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @PatchMapping(value = "/api/auth/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> change(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @Valid @RequestBody PasswordChangeRequest body) {
        service.change(account.userId(), account.authVersion(), body.accountProof(), body.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString()).build();
    }
}
