package com.daebangg.sayroam.auth.presentation.error;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.daebangg.sayroam.auth.presentation")
public class AuthExceptionHandler {
    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handle(AuthException exception) {
        HttpError error = httpError(exception.getErrorCode());
        var problem = ProblemDetail.forStatusAndDetail(error.status(), error.detail());
        problem.setProperty("code", exception.getErrorCode().name());
        var response = ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore());
        if (exception.getRetryAfter() != null) {
            long seconds = exception.getRetryAfter().toSeconds();
            if (exception.getRetryAfter().getNano() != 0) {
                seconds++;
            }
            problem.setProperty("retryAfterSeconds", seconds);
            response.header(HttpHeaders.RETRY_AFTER, Long.toString(seconds));
        }
        // cause・suppressedを含む内部情報は公開応答に含めない。
        return response.body(problem);
    }

    private HttpError httpError(AuthErrorCode code) {
        return switch (code) {
            case EMAIL_DELIVERY_DISABLED -> new HttpError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email delivery is disabled");
            case EMAIL_RATE_LIMITED -> new HttpError(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many verification email requests");
            case EMAIL_DELIVERY_FAILED -> new HttpError(HttpStatus.BAD_GATEWAY,
                    "Verification email could not be sent");
            case EMAIL_VERIFICATION_UNAVAILABLE -> new HttpError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification storage is unavailable");
            case EMAIL_REQUEST_SUPERSEDED -> new HttpError(HttpStatus.CONFLICT,
                    "Verification request expired or was replaced; use the latest request");
        };
    }

    private record HttpError(HttpStatus status, String detail) {
    }
}
