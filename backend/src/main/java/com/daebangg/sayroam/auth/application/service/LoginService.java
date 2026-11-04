package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.TokenPair;
import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.PasswordHasher;
import com.daebangg.sayroam.auth.application.port.out.RefreshTokenStore;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.RefreshToken;
import org.springframework.stereotype.Service;

@Service
public class LoginService {
    private final AccountLookup accounts;
    private final PasswordHasher passwords;
    private final AccessTokenIssuer tokens;
    private final RefreshTokenStore refreshTokens;

    public LoginService(AccountLookup accounts, PasswordHasher passwords, AccessTokenIssuer tokens,
                        RefreshTokenStore refreshTokens) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
    }

    public TokenPair login(String email, String password) {
        var account = accounts.findActiveCredentials(EmailAddress.normalize(email));
        boolean matches = passwords.matches(password, account.map(AccountLookup.Credentials::passwordHash).orElse(null));
        if (account.isEmpty() || !matches) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        long userId = account.orElseThrow().userId();
        RefreshToken refresh = RefreshToken.create();
        var access = tokens.issue(userId);
        // Redis 저장이 성공한 경우에만 두 토큰을 클라이언트에 전달한다.
        long expiresIn = refreshTokens.create(refresh.sessionId(), userId, refresh.digest());
        return new TokenPair(access, refresh.value(), expiresIn);
    }
}
