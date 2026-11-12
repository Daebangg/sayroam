package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.service.SocialLoginService;
import com.daebangg.sayroam.auth.presentation.api.SocialLoginApi;
import com.daebangg.sayroam.auth.presentation.cookie.OAuthBrowserCookie;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.LoginResponse;
import com.daebangg.sayroam.auth.presentation.dto.SocialAuthStartRequest;
import com.daebangg.sayroam.auth.presentation.dto.SocialAuthStartResponse;
import com.daebangg.sayroam.auth.presentation.dto.SocialExchangeResponse;
import com.daebangg.sayroam.auth.presentation.dto.SocialProvidersResponse;
import com.daebangg.sayroam.auth.presentation.dto.SocialResultExchangeRequest;
import com.daebangg.sayroam.auth.presentation.dto.SocialSignupRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth/social")
public class SocialLoginController implements SocialLoginApi {
    private final SocialLoginService login;
    private final OAuthBrowserCookie browserCookie;
    private final RefreshTokenCookie refreshCookie;

    public SocialLoginController(SocialLoginService login,
                                 OAuthBrowserCookie browserCookie, RefreshTokenCookie refreshCookie) {
        this.login = login;
        this.browserCookie = browserCookie;
        this.refreshCookie = refreshCookie;
    }

    @Override
    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialProvidersResponse> providers() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SocialProvidersResponse(login.availableProviders()));
    }

    @Override
    @PostMapping(value = "/{provider}/authorizations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialAuthStartResponse> start(@PathVariable String provider,
            @Valid @RequestBody SocialAuthStartRequest request, HttpServletRequest http) {
        String browser = browserCookie.prepare(http);
        SocialLoginService.Started started = login.start(login.parseProvider(provider), request.returnPath(), browser, Boolean.TRUE.equals(request.requestEmailConsent()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, browserCookie.issue(browser).toString())
                .body(new SocialAuthStartResponse(started.authorizationUrl(), started.expiresInSeconds()));
    }

    @Override
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
            @RequestParam(required = false) String state, @RequestParam(required = false) String code,
            @RequestParam(required = false) String error, HttpServletRequest http) {
        URI location = login.callback(login.parseProvider(provider), state, code, error, browserCookie.require(http));
        return ResponseEntity.status(302).cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").location(location).build();
    }

    @Override
    @PostMapping(value = "/results/exchange", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialExchangeResponse> exchange(@Valid @RequestBody SocialResultExchangeRequest request,
            HttpServletRequest http) {
        SocialLoginService.Exchanged exchanged = login.exchange(request.resultCode(), browserCookie.require(http));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (exchanged.tokens() != null) {
            TokenPairResult tokens = exchanged.tokens();
            return response.header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken(), tokens.refreshExpiresIn()).toString())
                    .body(new SocialExchangeResponse(SocialExchangeResponse.Status.AUTHENTICATED, loginResponse(tokens), null, exchanged.newlyRegistered()));
        }
        SocialLoginService.PendingSignup pending = exchanged.signup();
        return response.body(new SocialExchangeResponse(SocialExchangeResponse.Status.SIGNUP_REQUIRED, null,
                new SocialExchangeResponse.Signup(pending.signupTicket(), pending.expiresInSeconds(),
                        pending.emailVerificationRequired(), pending.suggestedEmail()), false));
    }

    @Override
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SocialSignupRequest request, HttpServletRequest http) {
        TokenPairResult tokens = login.signup(request.signupTicket(), browserCookie.require(http));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken(), tokens.refreshExpiresIn()).toString())
                .body(loginResponse(tokens));
    }

    private LoginResponse loginResponse(TokenPairResult tokens) {
        return new LoginResponse(tokens.access().accessToken(), "Bearer", tokens.access().expiresIn());
    }
}
