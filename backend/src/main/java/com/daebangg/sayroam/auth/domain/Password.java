package com.daebangg.sayroam.auth.domain;

import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;

/** 새 비밀번호는 입력받은 원문 그대로 검증하고 해싱한다. */
public record Password(String value) {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 24;
    public static final int LEGACY_MAX_LENGTH = 128;
    public static final String PATTERN = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[\\p{P}\\p{S}])[^\\s\\p{Z}\\p{C}]+$";

    public Password {
        if (value == null || value.length() < MIN_LENGTH || value.length() > MAX_LENGTH || !value.matches(PATTERN)) {
            throw new AuthException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
        }
    }

    @Override
    public String toString() {
        return "Password[redacted]";
    }
}
