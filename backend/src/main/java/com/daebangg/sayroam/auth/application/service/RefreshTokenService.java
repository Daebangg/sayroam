package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.dto.TokenPairResult;
import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.RefreshTokenStore;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.auth.domain.RefreshToken;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RefreshTokenService {
    private final RefreshTokenStore sessions;
    private final AccountLookup accounts;
    private final AccessTokenIssuer accessTokens;

    public RefreshTokenService(RefreshTokenStore sessions, AccountLookup accounts, AccessTokenIssuer accessTokens) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.accessTokens = accessTokens;
    }

    public TokenPairResult refresh(String rawToken) {
        RefreshToken current = RefreshToken.parse(rawToken)
                .orElseThrow(() -> new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID));
        String digest = current.digest();
        RefreshTokenStore.SessionResult inspected = requireActive(sessions.inspect(current.sessionId(), digest));
        Optional<AuthenticatedAccount> account = accounts.findActiveAccount(inspected.userId());
        if (account.isEmpty() || account.orElseThrow().authVersion() != inspected.authVersion()) {
            // DB 버전 불일치만으로 거절한다. Redis 정리 성공에 전체 로그인 차단을 의존하지 않는다.
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }
        // DB 확인·서명 실패 시에는 현재 토큰을 소비하지 않는다.
        AccessTokenIssuer.IssuedAccessToken access = accessTokens.issue(inspected.userId(), inspected.authVersion());
        RefreshToken next = current.next();
        // inspect 이후의 동시 요청·재사용·폐기도 여기서 다시 확인한다.
        RefreshTokenStore.SessionResult rotated = requireActive(sessions.rotate(current.sessionId(), digest, next.digest()));
        if (rotated.userId() != inspected.userId() || rotated.authVersion() != inspected.authVersion()
                || rotated.rememberMe() != inspected.rememberMe()) {
            throw new IllegalStateException("Refresh session account changed");
        }
        return new TokenPairResult(access, next.value(), rotated.expiresIn(), rotated.rememberMe());
    }

    private RefreshTokenStore.SessionResult requireActive(RefreshTokenStore.SessionResult result) {
        return switch (result.status()) {
            case ACTIVE -> result;
            case INVALID -> throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
            case REUSED -> throw new AuthException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        };
    }
}
