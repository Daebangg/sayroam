package com.daebangg.sayroam.auth.domain;

/** 가입·비밀번호 변경에서 공통으로 적용하는 원문 비밀번호 정책. 값을 정규화하거나 보관하지 않는다. */
public final class PasswordPolicy {
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 24;
    public static final int LEGACY_MAX_LENGTH = 128;
    public static final String PATTERN = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[\\p{P}\\p{S}])[^\\s\\p{Z}\\p{C}]+$";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        return password != null && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH
                && password.matches(PATTERN);
    }
}
