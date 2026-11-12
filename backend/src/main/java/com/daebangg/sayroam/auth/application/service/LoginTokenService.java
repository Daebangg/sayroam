package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.auth.application.port.out.RefreshTokenStore;
import com.daebangg.sayroam.auth.domain.RefreshToken;
import org.springframework.stereotype.Service;

/** 로그인 수단 검증을 마친 서비스가 공유하는 신규 로그인 토큰 발급 흐름. */
@Service
public class LoginTokenService {
    private final AccessTokenIssuer tokens;
    private final RefreshTokenStore refreshTokens;

    public LoginTokenService(AccessTokenIssuer tokens, RefreshTokenStore refreshTokens) {
        this.tokens = tokens;
        this.refreshTokens = refreshTokens;
    }

    public TokenPairResult issue(long userId, long authVersion) {
        return issue(userId, authVersion, true);
    }

    public TokenPairResult issue(long userId, long authVersion, boolean rememberMe) {
        RefreshToken refresh = RefreshToken.create();
        AccessTokenIssuer.IssuedAccessToken access = tokens.issue(userId, authVersion);
        // Redis 저장이 성공한 경우에만 두 토큰을 클라이언트에 전달한다.
        long expiresIn = refreshTokens.create(refresh.sessionId(), userId, refresh.digest(), authVersion, rememberMe);
        return new TokenPairResult(access, refresh.value(), expiresIn, rememberMe);
    }
}
