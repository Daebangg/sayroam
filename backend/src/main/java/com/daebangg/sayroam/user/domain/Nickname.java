package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.validation.InputText;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;

public record Nickname(String value) {
    public static final int MAX_LENGTH = 50;
    public static final String PATTERN = "^[^\\p{C}\\p{Zl}\\p{Zp}]+$";

    public Nickname {
        value = InputText.trim(value);
        if (value == null || value.isEmpty()) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_REQUIRED);
        }
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_TOO_LONG);
        }
        if (!value.matches(PATTERN)) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_INVALID);
        }
    }

    public static Nickname fromDisplayName(String displayName) {
        if (displayName == null) {
            return new Nickname("여행자");
        }
        String value = InputText.trim(displayName.replaceAll("[\\p{C}\\p{Zl}\\p{Zp}]", ""));
        if (value.isEmpty()) {
            return new Nickname("여행자");
        }
        if (value.length() > MAX_LENGTH) {
            int end = MAX_LENGTH;
            if (Character.isHighSurrogate(value.charAt(end - 1))) {
                end--;
            }
            value = value.substring(0, end);
        }
        return new Nickname(value);
    }
}
