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
            "Profile image URL must be an HTTP(S) URL or a supported avatar path"
    ),
    USER_PROFILE_IMAGE_INVALID(
            ErrorType.INVALID,
            "USER_PROFILE_IMAGE_INVALID",
            "Profile image must be a valid JPEG or PNG up to 2MB and 4096 pixels per side"
    ),
    USER_BIRTH_DATE_INVALID(
            ErrorType.INVALID,
            "USER_BIRTH_DATE_INVALID",
            "Birthday must be a valid date and not in the future"
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