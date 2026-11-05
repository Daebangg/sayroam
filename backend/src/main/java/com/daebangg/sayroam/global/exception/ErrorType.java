package com.daebangg.sayroam.global.exception;

/** 통신 방식과 무관한 실패 종류. 각 진입점이 자신의 응답 형식으로 변환한다. */
public enum ErrorType {
    INVALID,
    UNAUTHENTICATED,
    FORBIDDEN,
    CONFLICT,
    EXPIRED,
    RATE_LIMITED,
    UNAVAILABLE,
    EXTERNAL_FAILURE,
    INTERNAL
}
