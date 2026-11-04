package com.daebangg.sayroam.auth.application.port.out;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(long userId, long authVersion);

    record IssuedAccessToken(String accessToken, long expiresIn) {
        @Override
        public String toString() {
            return "IssuedAccessToken[redacted]";
        }
    }
}
