package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.service.SignupService;
import com.daebangg.sayroam.auth.presentation.api.SignupApi;
import com.daebangg.sayroam.auth.presentation.dto.SignupRequest;
import com.daebangg.sayroam.auth.presentation.dto.SignupResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/signup")
public class SignupController implements SignupApi {
    private final SignupService service;

    public SignupController(SignupService service) {
        this.service = service;
    }

    @Override
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest body) {
        long userId = service.signup(body.email(), body.password(), body.nickname(), body.signupProof());
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                .body(new SignupResponse(userId));
    }
}
