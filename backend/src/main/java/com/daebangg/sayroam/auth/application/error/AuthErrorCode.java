package com.daebangg.sayroam.auth.application.error;

/** 애플리케이션 실패 식별자. HTTP 상태와 공개 응답 문구는 presentation에서 정의한다. */
public enum AuthErrorCode {
    EMAIL_DELIVERY_DISABLED,
    EMAIL_RATE_LIMITED,
    EMAIL_DELIVERY_FAILED,
    EMAIL_VERIFICATION_UNAVAILABLE,
    EMAIL_REQUEST_SUPERSEDED,
    EMAIL_CODE_INVALID,
    EMAIL_CODE_EXPIRED,
    EMAIL_CODE_ATTEMPTS_EXCEEDED,
    EMAIL_CODE_ALREADY_USED
}
