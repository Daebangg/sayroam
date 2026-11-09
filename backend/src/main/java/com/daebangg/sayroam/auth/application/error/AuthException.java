package com.daebangg.sayroam.auth.application.error;

import com.daebangg.sayroam.global.exception.BusinessException;

import java.time.Duration;

/** 외부 기술과 HTTP에 의존하지 않는 인증 유스케이스의 실패 계약. */
public class AuthException extends BusinessException {
    private final Duration retryAfter;
    private final Integer remainingAttempts;

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
        this(errorCode, retryAfter, cause, null);
    }

    public AuthException(AuthErrorCode errorCode, int remainingAttempts) {
        this(errorCode, null, null, remainingAttempts);
        if (errorCode != AuthErrorCode.INVALID_CREDENTIALS || remainingAttempts < 1) {
            throw new IllegalArgumentException("Remaining attempts require a credential failure");
        }
    }

    private AuthException(AuthErrorCode errorCode, Duration retryAfter, Throwable cause, Integer remainingAttempts) {
        super(errorCode, cause);
        if (retryAfter != null && retryAfter.isNegative()) {
            throw new IllegalArgumentException("retryAfter must not be negative");
        }
        this.retryAfter = retryAfter;
        this.remainingAttempts = remainingAttempts;
    }

    @Override
    public AuthErrorCode getErrorCode() {
        return (AuthErrorCode) super.getErrorCode();
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }
}
