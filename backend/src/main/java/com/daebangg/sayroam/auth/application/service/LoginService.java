package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final AccountLookup accounts;
    private final PasswordHasher passwords;
    private final LoginTokenService tokens;

    public LoginService(AccountLookup accounts, PasswordHasher passwords, LoginTokenService tokens) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
    }

    public TokenPair login(String email, String password) {
        var account = accounts.findActiveCredentials(EmailAddress.normalize(email));
        boolean matches = passwords.matches(password, account.map(AccountLookup.Credentials::passwordHash).orElse(null));
        if (account.isEmpty() || !matches) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        var credentials = account.orElseThrow();
        return tokens.issue(credentials.userId(), credentials.authVersion());
    }
}
