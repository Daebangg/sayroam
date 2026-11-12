package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.presentation.api.AuthCsrfApi;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthCsrfController implements AuthCsrfApi {
    @Override
    @GetMapping("/api/auth/csrf")
    public ResponseEntity<Void> csrf(CsrfToken token) {
        token.getToken();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
