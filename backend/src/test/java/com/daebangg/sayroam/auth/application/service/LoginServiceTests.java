package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.LoginAttemptStore;
import com.daebangg.sayroam.auth.application.port.out.LoginAttemptStore.Outcome;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class LoginServiceTests {
    private final AccountLookup accounts = mock(AccountLookup.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final LoginTokenService tokens = mock(LoginTokenService.class);
    private final LoginAttemptStore attempts = mock(LoginAttemptStore.class);
    private final LoginService service = new LoginService(accounts, passwords, tokens, attempts);
    private static final String EMAIL = "member@example.test";
    private static final String IP = "192.0.2.85";

    @Test
    void blockedOrUnavailableStorageStopsBeforeAccountLookupAndPasswordHashing() {
        when(attempts.evaluate(EMAIL, IP, Outcome.CHECK)).thenReturn(new LoginAttemptStore.Evaluation(Duration.ofSeconds(300), 0));
        assertThatThrownBy(() -> service.login(" MEMBER@EXAMPLE.TEST ", "legacy-password", IP, true))
                .isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.getErrorCode()).isEqualTo(AuthErrorCode.LOGIN_RATE_LIMITED));
        when(attempts.evaluate(EMAIL, IP, Outcome.CHECK)).thenThrow(new AuthException(AuthErrorCode.LOGIN_ATTEMPTS_UNAVAILABLE));
        assertThatThrownBy(() -> service.login(EMAIL, "legacy-password", IP, true)).isInstanceOf(AuthException.class);
        verifyNoInteractions(accounts, passwords, tokens);
    }

    @Test
    void missingAccountsStillPerformDummyPasswordVerificationAndRecordAFailure() {
        when(attempts.evaluate(anyString(), anyString(), any())).thenReturn(new LoginAttemptStore.Evaluation(Duration.ZERO, 4));
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.login(EMAIL, "legacy-password", IP, true))
                .isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_CREDENTIALS));
        verify(passwords).matches("legacy-password", null);
        verify(attempts).evaluate(EMAIL, IP, Outcome.FAILURE);
        verifyNoInteractions(tokens);
    }

    @Test
    void concurrentBlockDuringCorrectPasswordVerificationPreventsTokenIssuance() {
        when(attempts.evaluate(EMAIL, IP, Outcome.CHECK)).thenReturn(new LoginAttemptStore.Evaluation(Duration.ZERO, 4));
        when(attempts.evaluate(EMAIL, IP, Outcome.SUCCESS)).thenReturn(new LoginAttemptStore.Evaluation(Duration.ofSeconds(299), 0));
        when(accounts.findActiveCredentials(EMAIL)).thenReturn(Optional.of(new AccountLookup.Credentials(1, "hash", 0)));
        when(passwords.matches("legacy-password", "hash")).thenReturn(true);
        assertThatThrownBy(() -> service.login(EMAIL, "legacy-password", IP, false))
                .isInstanceOfSatisfying(AuthException.class, error -> assertThat(error.getErrorCode()).isEqualTo(AuthErrorCode.LOGIN_RATE_LIMITED));
        verifyNoInteractions(tokens);
    }
}
