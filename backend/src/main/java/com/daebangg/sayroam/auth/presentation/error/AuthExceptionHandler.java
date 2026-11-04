package com.daebangg.sayroam.auth.presentation.error;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
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
    private final RefreshTokenCookie refreshCookie;

    public AuthExceptionHandler(RefreshTokenCookie refreshCookie) {
        this.refreshCookie = refreshCookie;
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ProblemDetail> handle(AuthException exception) {
        HttpError error = httpError(exception.getErrorCode());
        var problem = ProblemDetail.forStatusAndDetail(error.status(), error.detail());
        problem.setProperty("code", exception.getErrorCode().name());
        var response = ResponseEntity.status(error.status()).cacheControl(CacheControl.noStore());
        if (exception.getErrorCode() == AuthErrorCode.REFRESH_TOKEN_INVALID
                || exception.getErrorCode() == AuthErrorCode.REFRESH_TOKEN_REUSED
                || exception.getErrorCode() == AuthErrorCode.PASSWORD_CHANGE_UNAVAILABLE) {
            response.header(HttpHeaders.SET_COOKIE, refreshCookie.clear().toString());
        }
        if (exception.getErrorCode() == AuthErrorCode.AUTHENTICATION_REVOKED) {
            response.header(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }
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
            case PASSWORD_POLICY_VIOLATION -> new HttpError(HttpStatus.BAD_REQUEST,
                    "Password must contain %d to %d characters and must not be blank"
                            .formatted(PasswordPolicy.MIN_LENGTH, PasswordPolicy.MAX_LENGTH));
            case AUTHENTICATION_REVOKED -> new HttpError(HttpStatus.UNAUTHORIZED,
                    "Authentication was revoked; sign in again");
            case PASSWORD_CHANGE_NOT_ALLOWED -> new HttpError(HttpStatus.FORBIDDEN,
                    "Password change requires an existing password login; adding a password is not supported here");
            case PASSWORD_CHANGE_CONFLICT -> new HttpError(HttpStatus.CONFLICT,
                    "Account state changed; sign in again and obtain a new verification proof before retrying");
            case PASSWORD_CHANGE_UNAVAILABLE -> new HttpError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Password change could not be confirmed; the proof is not restored. Clear local login state and sign in again to confirm the outcome before retrying");
            case ACCOUNT_VERIFICATION_NOT_ALLOWED -> new HttpError(HttpStatus.FORBIDDEN,
                    "An active account with a verified email is required for account verification");
            case ACCOUNT_PROOF_INVALID -> new HttpError(HttpStatus.BAD_REQUEST,
                    "Account proof is invalid for this account, email or purpose");
            case ACCOUNT_PROOF_EXPIRED -> new HttpError(HttpStatus.GONE,
                    "Account proof expired or is unavailable; verify your email again");
            case ACCOUNT_PROOF_ALREADY_USED -> new HttpError(HttpStatus.CONFLICT,
                    "Account proof was already used; verify your email again before another attempt");
            case REFRESH_TOKEN_INVALID -> new HttpError(HttpStatus.UNAUTHORIZED,
                    "Refresh token is invalid or expired; sign in again");
            case REFRESH_TOKEN_REUSED -> new HttpError(HttpStatus.UNAUTHORIZED,
                    "Refresh token was reused; this login can no longer be refreshed. Sign in again");
            case INVALID_CREDENTIALS -> new HttpError(HttpStatus.UNAUTHORIZED,
                    "Email or password is incorrect");
            case AUTHENTICATION_UNAVAILABLE -> new HttpError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Authentication is temporarily unavailable");
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
            case EMAIL_CODE_INVALID -> new HttpError(HttpStatus.BAD_REQUEST, "Verification code is invalid");
            case EMAIL_CODE_EXPIRED -> new HttpError(HttpStatus.GONE,
                    "Verification code expired or is unavailable; request a new code");
            case EMAIL_CODE_ATTEMPTS_EXCEEDED -> new HttpError(HttpStatus.TOO_MANY_REQUESTS,
                    "Verification attempt limit reached; request a new code");
            case EMAIL_CODE_ALREADY_USED -> new HttpError(HttpStatus.CONFLICT,
                    "Verification code was already used; use the issued proof or request a new code");
            case SIGNUP_PROOF_INVALID -> new HttpError(HttpStatus.BAD_REQUEST,
                    "Signup proof is invalid for this email");
            case SIGNUP_PROOF_EXPIRED -> new HttpError(HttpStatus.GONE,
                    "Signup proof expired or is unavailable; verify your email again");
            case SIGNUP_PROOF_ALREADY_USED -> new HttpError(HttpStatus.CONFLICT,
                    "Signup proof was already used; verify your email again if signup did not complete");
            case EMAIL_ALREADY_REGISTERED -> new HttpError(HttpStatus.CONFLICT,
                    "Email is already registered");
            case SIGNUP_UNAVAILABLE -> new HttpError(HttpStatus.SERVICE_UNAVAILABLE,
                    "Signup could not be confirmed; the proof is not restored. Verify your email again before retrying; the account may already exist");
        };
    }

    private record HttpError(HttpStatus status, String detail) {
    }
}
