package com.daebangg.sayroam.user.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NICKNAME_REQUIRED(
            ErrorType.INVALID,
            "USER_NICKNAME_REQUIRED",
            "Nickname is required"
    ),
    USER_NICKNAME_TOO_LONG(
            ErrorType.INVALID,
            "USER_NICKNAME_TOO_LONG",
            "Nickname must be 50 characters or less"
    ),
    USER_NICKNAME_INVALID(
            ErrorType.INVALID,
            "USER_NICKNAME_INVALID",
            "Nickname must not contain control characters"
    ),
    USER_PROFILE_IMAGE_URL_TOO_LONG(
            ErrorType.INVALID,
            "USER_PROFILE_IMAGE_URL_TOO_LONG",
            "Profile image URL must be 500 characters or less"
    ),
    USER_PROFILE_IMAGE_URL_INVALID(
            ErrorType.INVALID,
            "USER_PROFILE_IMAGE_URL_INVALID",
            "Profile image URL must be a valid HTTP or HTTPS URL"
    ),
    USER_BIRTH_YEAR_INVALID(
            ErrorType.INVALID,
            "USER_BIRTH_YEAR_INVALID",
            "Birth year must be positive and not in the future"
    );

    private final ErrorType type;
    private final String code;
    private final String message;
}