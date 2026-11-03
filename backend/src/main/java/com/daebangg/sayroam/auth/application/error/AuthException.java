package com.daebangg.sayroam.auth.application.error;

import java.time.Duration;
import java.util.Objects;

/** 외부 기술과 HTTP에 의존하지 않는 인증 유스케이스의 실패 계약. */
public class AuthException extends RuntimeException {
    private final AuthErrorCode errorCode;
    private final Duration retryAfter;

    public AuthException(AuthErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public AuthException(AuthErrorCode errorCode, Throwable cause) {
        this(errorCode, null, cause);
    }

    public AuthException(AuthErrorCode errorCode, Duration retryAfter) {
        this(errorCode, retryAfter, null);
    }

    private AuthException(AuthErrorCode errorCode, Duration retryAfter, Throwable cause) {
        super(Objects.requireNonNull(errorCode).name(), cause);
        if (retryAfter != null && retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
        this.errorCode = errorCode;
        this.retryAfter = retryAfter;
    }

    public AuthErrorCode getErrorCode() {
        return errorCode;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
