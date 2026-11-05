package com.daebangg.sayroam.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** MVC와 Security 필터가 공유하는 오류 본문 생성 규칙. */
public final class ApiProblems {
    private ApiProblems() {
    }

    public static ProblemDetail of(ErrorCode code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(toHttpStatus(code.getType()), code.getMessage());
        problem.setProperty("code", code.getCode());
        return problem;
    }

    private static HttpStatus toHttpStatus(ErrorType type) {
        return switch (type) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case CONFLICT -> HttpStatus.CONFLICT;
            case EXPIRED -> HttpStatus.GONE;
            case RATE_LIMITED -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case EXTERNAL_FAILURE -> HttpStatus.BAD_GATEWAY;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
