package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.application.port.out.LoginAttemptStore;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LoginService {
    private final AccountLookup accounts;
    private final PasswordHasher passwords;
    private final LoginTokenService tokens;
    private final LoginAttemptStore attempts;

    public LoginService(AccountLookup accounts, PasswordHasher passwords, LoginTokenService tokens, LoginAttemptStore attempts) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
        this.attempts = attempts;
    }

    public TokenPair login(String email, String password, String remoteAddress, boolean rememberMe) {
        String normalized = EmailAddress.normalize(email);
        requireAllowed(normalized, remoteAddress, LoginAttemptStore.Outcome.CHECK);
        Optional<AccountLookup.Credentials> account = accounts.findActiveCredentials(normalized);
        boolean matches = passwords.matches(password, account.map(AccountLookup.Credentials::passwordHash).orElse(null));
        if (account.isEmpty() || !matches) {
            LoginAttemptStore.Evaluation failed = requireAllowed(normalized, remoteAddress, LoginAttemptStore.Outcome.FAILURE);
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS, failed.remainingAttempts());
        }
        requireAllowed(normalized, remoteAddress, LoginAttemptStore.Outcome.SUCCESS);
        AccountLookup.Credentials credentials = account.orElseThrow();
        return tokens.issue(credentials.userId(), credentials.authVersion(), rememberMe);
    }

    private LoginAttemptStore.Evaluation requireAllowed(String email, String remoteAddress, LoginAttemptStore.Outcome outcome) {
        LoginAttemptStore.Evaluation result = attempts.evaluate(email, remoteAddress, outcome);
        if (!result.retryAfter().isZero()) throw new AuthException(AuthErrorCode.LOGIN_RATE_LIMITED, result.retryAfter());
        return result;
    }
}
