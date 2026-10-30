package com.daebangg.sayroam.auth.application;

import com.daebangg.sayroam.global.exception.BusinessException;

public class EmailVerificationException extends BusinessException {
    private final Long retryAfterSeconds;

    public EmailVerificationException(AuthErrorCode code) {
        this(code, null);
    }

    public EmailVerificationException(AuthErrorCode code, Long retryAfterSeconds) {
        super(code);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
