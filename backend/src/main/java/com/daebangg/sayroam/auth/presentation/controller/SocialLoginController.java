package com.daebangg.sayroam.auth.presentation.controller;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.service.SocialLoginService;
import com.daebangg.sayroam.auth.application.service.SocialSignupService;
import com.daebangg.sayroam.auth.presentation.cookie.OAuthBrowserCookie;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.auth.presentation.dto.*;
import com.daebangg.sayroam.user.domain.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Locale;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/auth/social")
@ApiResponses({
        @ApiResponse(responseCode = "400", ref = "#/components/responses/SocialBadRequest"),
        @ApiResponse(responseCode = "401", ref = "#/components/responses/SocialUnauthorized"),
        @ApiResponse(responseCode = "403", ref = "#/components/responses/Forbidden"),
        @ApiResponse(responseCode = "409", ref = "#/components/responses/SocialConflict"),
        @ApiResponse(responseCode = "503", ref = "#/components/responses/SocialUnavailable"),
        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalServerError")
})
public class SocialLoginController {
    private final SocialLoginService login;
    private final SocialSignupService signup;
    private final OAuthBrowserCookie browserCookie;
    private final RefreshTokenCookie refreshCookie;

    @Operation(operationId = "listSocialProviders", summary = "사용 가능한 소셜 로그인 공급자",
            description = "인증 없이 현재 서버에서 활성화한 공급자 목록만 반환합니다. 자격 정보는 공개하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "활성 공급자 목록. Cache-Control: no-store")
    @GetMapping(value = "/providers", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialProvidersResponse> providers() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new SocialProvidersResponse(login.availableProviders()));
    }

    public SocialLoginController(SocialLoginService login, SocialSignupService signup,
                                 OAuthBrowserCookie browserCookie, RefreshTokenCookie refreshCookie) {
        this.login = login;
        this.signup = signup;
        this.browserCookie = browserCookie;
        this.refreshCookie = refreshCookie;
    }

    @Operation(operationId = "startSocialLogin", summary = "소셜 로그인 인증 시작",
            description = "활성화한 Google·Kakao·Naver만 사용합니다. Google·Kakao는 계정 선택을 요청하고 Naver는 재로그인 화면을 요청합니다. Naver 이메일 재동의 요청은 reprompt를 사용합니다. CSRF 쿠키·헤더 필요. HttpOnly 브라우저 바인딩 쿠키를 설정하고 state 및 지원 공급자의 PKCE S256 인증 주소를 반환합니다. 같은 브라우저에서 authorizationUrl로 이동하세요. returnPath는 서버 허용 목록만 사용합니다. Bearer 헤더 없이 호출하세요.")
    @ApiResponse(responseCode = "200", description = "인증 주소와 유효기간. Cache-Control: no-store",
            headers = @Header(name = "Set-Cookie", description = "__Host-sayroam_oauth: HttpOnly, Secure, SameSite=Lax, Path=/. HTTP local에서는 sayroam_oauth이며 Secure=false", schema = @Schema(type = "string")))
    @PostMapping(value = "/{provider}/authorizations", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialAuthStartResponse> start(@PathVariable String provider,
            @Valid @RequestBody SocialAuthStartRequest request, HttpServletRequest http) {
        String browser = browserCookie.prepare(http);
        SocialLoginService.Started started = login.start(provider(provider), request.returnPath(), browser, Boolean.TRUE.equals(request.requestEmailConsent()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, browserCookie.issue(browser).toString())
                .body(new SocialAuthStartResponse(started.authorizationUrl(), started.expiresInSeconds()));
    }

    @Operation(operationId = "socialLoginCallback", summary = "공급자 인증 콜백",
            extensions = @Extension(properties = @ExtensionProperty(name = "x-browser-navigation", value = "true", parseValue = true)),
            description = "공급자가 GET 리다이렉트로 호출합니다. state·공급자·브라우저 바인딩을 검사해 일회성 소비합니다. 검증한 콜백만 허용된 returnPath#social_result=<일회성 코드>로 302 이동합니다. 바인딩 오류는 리다이렉트하지 않고 ProblemDetail을 반환합니다. 기존 Refresh 쿠키·Bearer 헤더·세션에 의존하지 않습니다.")
    @ApiResponse(responseCode = "302", description = "허용한 화면으로 이동. URL에 Access/Refresh Token 없음",
            headers = @Header(name = "Location", schema = @Schema(type = "string")))
    @GetMapping("/{provider}/callback")
    public ResponseEntity<Void> callback(@PathVariable String provider,
            @RequestParam(required = false) String state, @RequestParam(required = false) String code,
            @RequestParam(required = false) String error, HttpServletRequest http) {
        URI location = login.callback(provider(provider), state, code, error, browserCookie.require(http));
        return ResponseEntity.status(302).cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer").location(location).build();
    }

    @Operation(operationId = "exchangeSocialLoginResult", summary = "소셜 로그인 결과 일회성 교환",
            description = "인증을 시작한 브라우저의 HttpOnly 바인딩 쿠키와 CSRF 쿠키·헤더 필요. social_result 값을 읽은 뒤 주소에서 제거하세요. 소셜이 검증한 이메일이 기존 활성·이메일 인증 회원과 일치하면 결과 소비 시 같은 회원에 소셜을 연결하고 추가 로그인 없이 AUTHENTICATED를 반환합니다. AUTHENTICATED이면 login 본문과 Refresh 쿠키, 신규 회원도 공급자 이름·별명으로 가입하고 같은 응답에서 AUTHENTICATED를 반환합니다. 이름이 없으면 기본 닉네임을 사용하며 닉네임 입력 화면은 없습니다. 이메일이 없거나 사용할 수 없으면 SOCIAL_EMAIL_REQUIRED로 제공 동의를 안내하며 인증 메일은 보내지 않습니다. 실패 결과는 해당 ProblemDetail로 반환합니다. 소비 후 응답 유실·장애 시 자동 재시도하지 말고 소셜 인증을 다시 시작하세요.")
    @ApiResponse(responseCode = "200", description = "로그인 또는 가입 대기. Cache-Control: no-store")
    @PostMapping(value = "/results/exchange", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SocialExchangeResponse> exchange(@Valid @RequestBody SocialResultExchangeRequest request,
            HttpServletRequest http) {
        SocialLoginService.Exchanged exchanged = login.exchange(request.resultCode(), browserCookie.require(http));
        ResponseEntity.BodyBuilder response = ResponseEntity.ok().cacheControl(CacheControl.noStore());
        if (exchanged.tokens() != null) {
            TokenPair tokens = exchanged.tokens();
            return response.header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken(), tokens.refreshExpiresIn()).toString())
                    .body(new SocialExchangeResponse(SocialExchangeResponse.Status.AUTHENTICATED, loginResponse(tokens), null, exchanged.newlyRegistered()));
        }
        SocialLoginService.PendingSignup pending = exchanged.signup();
        return response.body(new SocialExchangeResponse(SocialExchangeResponse.Status.SIGNUP_REQUIRED, null,
                new SocialExchangeResponse.Signup(pending.signupTicket(), pending.expiresInSeconds(),
                        pending.emailVerificationRequired(), pending.suggestedEmail()), false));
    }

    @Operation(operationId = "completeSocialSignup", summary = "이전 소셜 가입 티켓 완료·로그인",
            description = "동일 브라우저 바인딩과 CSRF 쿠키·헤더 필요. 이전 버전에서 발급한 가입 티켓만 전달합니다. 닉네임은 공급자 이름·별명으로 정합니다. 이메일은 공급자 UserInfo에서 사용하며 인증 메일·이메일 코드·signupProof는 요구하지 않습니다. 비밀번호 자격 정보 없이 User+소셜 계정을 한 DB 트랜잭션에서 생성합니다. 같은 이메일의 기존 활성 회원은 그 계정에 연결하고 로그인합니다. 티켓 소비 또는 DB 커밋 이후 실패 시 복원·자동 재시도하지 않습니다.")
    @ApiResponse(responseCode = "200", description = "가입 커밋 후 Access 본문과 Refresh 쿠키 발급. Cache-Control: no-store")
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> signup(@Valid @RequestBody SocialSignupRequest request, HttpServletRequest http) {
        TokenPair tokens = signup.signup(request.signupTicket(), browserCookie.require(http));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.issue(tokens.refreshToken(), tokens.refreshExpiresIn()).toString())
                .body(loginResponse(tokens));
    }

    private SocialProvider provider(String value) {
        try {
            return SocialProvider.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_DISABLED);
        }
    }

    private LoginResponse loginResponse(TokenPair tokens) {
        return new LoginResponse(tokens.access().accessToken(), "Bearer", tokens.access().expiresIn());
    }
}
