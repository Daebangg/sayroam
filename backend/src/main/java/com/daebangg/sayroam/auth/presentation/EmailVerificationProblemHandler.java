package com.daebangg.sayroam.auth.presentation;

import com.daebangg.sayroam.auth.application.EmailVerificationException;
import com.daebangg.sayroam.global.exception.ApiProblems;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EmailVerificationController.class)
public class EmailVerificationProblemHandler {
    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ProblemDetail> handle(EmailVerificationException exception) {
        var problem = ApiProblems.of(exception.getErrorCode());
        var response = ResponseEntity.status(exception.getErrorCode().getStatus()).cacheControl(CacheControl.noStore());
        if (exception.getRetryAfterSeconds() != null) {
            problem.setProperty("retryAfterSeconds", exception.getRetryAfterSeconds());
            response.header(HttpHeaders.RETRY_AFTER, exception.getRetryAfterSeconds().toString());
        }
        return response.body(problem);
    }
}
