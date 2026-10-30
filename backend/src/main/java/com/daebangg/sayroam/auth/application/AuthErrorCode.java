package com.daebangg.sayroam.auth.application;

import com.daebangg.sayroam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    EMAIL_DELIVERY_DISABLED(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_DELIVERY_DISABLED", "Email delivery is disabled"),
    EMAIL_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_RATE_LIMITED", "Too many verification email requests"),
    EMAIL_DELIVERY_FAILED(HttpStatus.BAD_GATEWAY, "EMAIL_DELIVERY_FAILED", "Verification email could not be sent"),
    EMAIL_VERIFICATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "EMAIL_VERIFICATION_UNAVAILABLE", "Email verification storage is unavailable"),
    EMAIL_REQUEST_SUPERSEDED(HttpStatus.CONFLICT, "EMAIL_REQUEST_SUPERSEDED", "Verification request expired or was replaced; use the latest request");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
