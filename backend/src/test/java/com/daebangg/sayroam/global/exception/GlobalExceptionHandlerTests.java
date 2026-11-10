package com.daebangg.sayroam.global.exception;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.service.LoginService;
import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.controller.LoginController;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class GlobalExceptionHandlerTests {
    private LoginService login;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        login = mock(LoginService.class);
        RefreshTokenCookie refreshCookie = new RefreshTokenCookie(new RefreshCookieProperties(true));
        mvc = standaloneSetup(new LoginController(login, refreshCookie))
                .setControllerAdvice(new GlobalExceptionHandler(refreshCookie))
                .build();
    }

    // #47 이전 응답 계약을 고정한다. 새 ErrorType 매핑이나 enum의 메시지에서 기대값을 가져오지 않는다.
    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            EMAIL_DELIVERY_DISABLED|503|Email delivery is disabled|false|false
            EMAIL_RATE_LIMITED|429|Too many verification email requests|false|false
            EMAIL_DELIVERY_FAILED|502|Verification email could not be sent|false|false
            EMAIL_VERIFICATION_UNAVAILABLE|503|Email verification storage is unavailable|false|false
            EMAIL_REQUEST_SUPERSEDED|409|Verification request expired or was replaced; use the latest request|false|false
            EMAIL_CODE_INVALID|400|Verification code is invalid|false|false
            EMAIL_CODE_EXPIRED|410|Verification code expired or is unavailable; request a new code|false|false
            EMAIL_CODE_ATTEMPTS_EXCEEDED|429|Verification attempt limit reached; request a new code|false|false
            EMAIL_CODE_ALREADY_USED|409|Verification code was already used; use the issued proof or request a new code|false|false
            SIGNUP_PROOF_INVALID|400|Signup proof is invalid for this email|false|false
            SIGNUP_PROOF_EXPIRED|410|Signup proof expired or is unavailable; verify your email again|false|false
            SIGNUP_PROOF_ALREADY_USED|409|Signup proof was already used; verify your email again if signup did not complete|false|false
            EMAIL_ALREADY_REGISTERED|409|Email is already registered|false|false
            SIGNUP_UNAVAILABLE|503|Signup could not be confirmed; the proof is not restored. Verify your email again before retrying; the account may already exist|false|false
            INVALID_CREDENTIALS|401|Email or password is incorrect|false|false
            PASSWORD_POLICY_VIOLATION|400|Password must contain 8 to 24 characters, an uppercase letter, a number and a special character, without whitespace or invisible characters|false|false
            AUTHENTICATION_UNAVAILABLE|503|Authentication is temporarily unavailable|false|false
            REFRESH_TOKEN_INVALID|401|Refresh token is invalid or expired; sign in again|true|false
            REFRESH_TOKEN_REUSED|401|Refresh token was reused; this login can no longer be refreshed. Sign in again|true|false
            ACCOUNT_VERIFICATION_NOT_ALLOWED|403|An active account with a verified email is required for account verification|false|false
            ACCOUNT_PROOF_INVALID|400|Account proof is invalid for this account, email or purpose|false|false
            ACCOUNT_PROOF_EXPIRED|410|Account proof expired or is unavailable; verify your email again|false|false
            ACCOUNT_PROOF_ALREADY_USED|409|Account proof was already used; verify your email again before another attempt|false|false
            AUTHENTICATION_REVOKED|401|Authentication was revoked; sign in again|false|true
            PASSWORD_CHANGE_NOT_ALLOWED|403|Password change requires an existing password login; adding a password is not supported here|false|false
            PASSWORD_CHANGE_CONFLICT|409|Account state changed; sign in again and obtain a new verification proof before retrying|false|false
            PASSWORD_CHANGE_UNAVAILABLE|503|Password change could not be confirmed; the proof is not restored. Clear local login state and sign in again to confirm the outcome before retrying|true|false
            ACCOUNT_DELETION_CONFLICT|409|Account state changed; if the account is still active, sign in and verify your email again before retrying|false|false
            ACCOUNT_DELETION_UNAVAILABLE|503|Account deletion could not be confirmed; the proof is not restored. Clear local login state and do not retry or create a new account automatically|true|false
            """)
    void 기존_인증_오류의_본문과_쿠키_헤더_계약을_유지한다(
            AuthErrorCode code, int expectedStatus, String detail,
            boolean clearCookie, boolean bearerChallenge) throws Exception {
        given(login.login(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).willThrow(new AuthException(code));

        ResultActions result = mvc.perform(loginRequest())
                .andExpect(status().is(expectedStatus))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.title").value(HttpStatus.valueOf(expectedStatus).getReasonPhrase()))
                .andExpect(jsonPath("$.type").doesNotExist())
                .andExpect(jsonPath("$.instance").value("/api/auth/login"))
                .andExpect(jsonPath("$.code").value(code.name()))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.retryAfterSeconds").doesNotExist());
        if (clearCookie) {
            result.andExpect(cookie().maxAge(RefreshTokenCookie.NAME, 0))
                    .andExpect(cookie().value(RefreshTokenCookie.NAME, ""))
                    .andExpect(cookie().httpOnly(RefreshTokenCookie.NAME, true))
                    .andExpect(cookie().secure(RefreshTokenCookie.NAME, true))
                    .andExpect(cookie().path(RefreshTokenCookie.NAME, "/api/auth"))
                    .andExpect(header().string(HttpHeaders.SET_COOKIE,
                            containsString("SameSite=Strict")));
        } else {
            result.andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
        }
        if (bearerChallenge) {
            result.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
        } else {
            result.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
        }
    }

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 1", "1000000000, 1", "1000000001, 2"})
    void 재시도_시간을_초_단위로_올려_헤더와_본문에_전달한다(long nanos, long seconds) throws Exception {
        AuthException exception = new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, Duration.ofNanos(nanos));
        given(login.login(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).willThrow(exception);

        mvc.perform(loginRequest())
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string(HttpHeaders.RETRY_AFTER, Long.toString(seconds)))
                .andExpect(jsonPath("$.retryAfterSeconds").value(seconds))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void 원인과_suppressed_예외는_보존하지만_응답에는_노출하지_않는다() throws Exception {
        IllegalStateException cause = new IllegalStateException("private-database-connection");
        IllegalStateException suppressed = new IllegalStateException("private-mail-response");
        AuthException exception = new AuthException(AuthErrorCode.AUTHENTICATION_UNAVAILABLE, cause);
        exception.addSuppressed(suppressed);
        given(login.login(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean())).willThrow(exception);

        MvcResult result = mvc.perform(loginRequest())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Authentication is temporarily unavailable"))
                .andReturn();

        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception).isInstanceOf(BusinessException.class);
        assertThat(exception.getSuppressed()).containsExactly(suppressed);
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain("private-database-connection", "private-mail-response",
                        "cause", "suppressed", "stackTrace");
    }

    @Test
    void 일반_업무_예외에는_인증_전용_쿠키나_헤더를_추가하지_않는다() throws Exception {
        IllegalStateException cause = new IllegalStateException("private-persistence-error");
        BusinessException exception = new BusinessException(CommonErrorCode.INVALID_INPUT, cause);
        given(login.login(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean()))
                .willThrow(exception);

        mvc.perform(loginRequest())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.detail").value("Invalid input value"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
                .andExpect(header().doesNotExist(HttpHeaders.RETRY_AFTER));
        assertThat(exception.getCause()).isSameAs(cause);
    }

    private MockHttpServletRequestBuilder loginRequest() {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"user@example.com","password":"Test-password-123"}
                        """);
    }
}
