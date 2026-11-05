package com.daebangg.sayroam.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    UNAUTHENTICATED(ErrorType.UNAUTHENTICATED, "UNAUTHENTICATED", "Authentication is required"),
    AUTHENTICATION_UNAVAILABLE(ErrorType.UNAVAILABLE, "AUTHENTICATION_UNAVAILABLE", "Authentication is temporarily unavailable"),
    ACCESS_DENIED(ErrorType.FORBIDDEN, "ACCESS_DENIED", "Access is denied"),
    INVALID_CSRF_TOKEN(ErrorType.FORBIDDEN, "INVALID_CSRF_TOKEN", "CSRF token is missing or invalid"),
    INVALID_INPUT(ErrorType.INVALID, "INVALID_INPUT", "Invalid input value"),
    INTERNAL_SERVER_ERROR(ErrorType.INTERNAL, "INTERNAL_SERVER_ERROR", "An unexpected error occurred");

    private final ErrorType type;
    private final String code;
    private final String message;

}
