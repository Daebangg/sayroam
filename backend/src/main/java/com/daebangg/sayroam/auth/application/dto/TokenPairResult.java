package com.daebangg.sayroam.auth.application.dto;

import com.daebangg.sayroam.auth.application.port.out.AccessTokenIssuer;

/** 전송 방식과 독립적인 인증 결과. Refresh Token은 Presentation에서 쿠키로만 전달한다. */
public record TokenPairResult(AccessTokenIssuer.IssuedAccessToken access, String refreshToken, long refreshExpiresIn,
                        boolean rememberMe) {
    public TokenPairResult(AccessTokenIssuer.IssuedAccessToken access, String refreshToken, long refreshExpiresIn) {
        this(access, refreshToken, refreshExpiresIn, true);
    }
    @Override
    public String toString() {
        return "TokenPairResult[redacted]";
    }
}
