package com.daebangg.sayroam.global.exception;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class ApiProblemsTest {

    @ParameterizedTest
    @CsvSource({
            "UNAUTHENTICATED, 401",
            "AUTHENTICATION_UNAVAILABLE, 503",
            "ACCESS_DENIED, 403",
            "INVALID_CSRF_TOKEN, 403",
            "INVALID_INPUT, 400",
            "INTERNAL_SERVER_ERROR, 500"
    })
    void 공통_에러_코드의_HTTP_상태와_응답_코드를_유지한다(CommonErrorCode code, int status) {
        // when
        ProblemDetail problem = ApiProblems.of(code);

        // then
        assertThat(problem.getStatus()).isEqualTo(status);
        assertThat(problem.getDetail()).isEqualTo(code.getMessage());
        assertThat(problem.getProperties()).containsEntry("code", code.name());
    }

    @ParameterizedTest
    @CsvSource({
            "INVALID, 400",
            "UNAUTHENTICATED, 401",
            "FORBIDDEN, 403",
            "CONFLICT, 409",
            "EXPIRED, 410",
            "RATE_LIMITED, 429",
            "UNAVAILABLE, 503",
            "EXTERNAL_FAILURE, 502",
            "INTERNAL, 500"
    })
    void 실패_종류를_HTTP_상태로_바꾼다(ErrorType type, int status) {
        // given
        ErrorCode code = new TestErrorCode(type, "TEST_CODE", "test message");

        // when
        ProblemDetail problem = ApiProblems.of(code);

        // then
        assertThat(problem.getStatus()).isEqualTo(status);
    }

    private record TestErrorCode(ErrorType getType, String getCode, String getMessage) implements ErrorCode {
    }
}
