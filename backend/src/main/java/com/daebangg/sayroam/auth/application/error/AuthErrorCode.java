package com.daebangg.sayroam.auth.application.error;

import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 인증 실패의 종류·코드·공개 문구. HTTP 상태는 공통 웹 응답 규칙에서 결정한다. */
@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    EMAIL_DELIVERY_DISABLED(ErrorType.UNAVAILABLE,
            "Email delivery is disabled"),
    EMAIL_RATE_LIMITED(ErrorType.RATE_LIMITED,
            "Too many verification email requests"),
    EMAIL_DELIVERY_FAILED(ErrorType.EXTERNAL_FAILURE,
            "Verification email could not be sent"),
    EMAIL_VERIFICATION_UNAVAILABLE(ErrorType.UNAVAILABLE,
            "Email verification storage is unavailable"),
    EMAIL_REQUEST_SUPERSEDED(ErrorType.CONFLICT,
            "Verification request expired or was replaced; use the latest request"),
    EMAIL_CODE_INVALID(ErrorType.INVALID,
            "Verification code is invalid"),
    EMAIL_CODE_EXPIRED(ErrorType.EXPIRED,
            "Verification code expired or is unavailable; request a new code"),
    EMAIL_CODE_ATTEMPTS_EXCEEDED(ErrorType.RATE_LIMITED,
            "Verification attempt limit reached; request a new code"),
    EMAIL_CODE_ALREADY_USED(ErrorType.CONFLICT,
            "Verification code was already used; use the issued proof or request a new code"),
    SIGNUP_PROOF_INVALID(ErrorType.INVALID,
            "Signup proof is invalid for this email"),
    SIGNUP_PROOF_EXPIRED(ErrorType.EXPIRED,
            "Signup proof expired or is unavailable; verify your email again"),
    SIGNUP_PROOF_ALREADY_USED(ErrorType.CONFLICT,
            "Signup proof was already used; verify your email again if signup did not complete"),
    EMAIL_ALREADY_REGISTERED(ErrorType.CONFLICT,
            "Email is already registered"),
    SIGNUP_UNAVAILABLE(ErrorType.UNAVAILABLE,
            "Signup could not be confirmed; the proof is not restored. Verify your email again before retrying; the account may already exist"),
    INVALID_CREDENTIALS(ErrorType.UNAUTHENTICATED,
            "Email or password is incorrect"),
    PASSWORD_POLICY_VIOLATION(ErrorType.INVALID,
            "Password must contain %d to %d characters and must not be blank"
                    .formatted(PasswordPolicy.MIN_LENGTH, PasswordPolicy.MAX_LENGTH)),
    AUTHENTICATION_UNAVAILABLE(ErrorType.UNAVAILABLE,
            "Authentication is temporarily unavailable"),
    REFRESH_TOKEN_INVALID(ErrorType.UNAUTHENTICATED,
            "Refresh token is invalid or expired; sign in again"),
    REFRESH_TOKEN_REUSED(ErrorType.UNAUTHENTICATED,
            "Refresh token was reused; this login can no longer be refreshed. Sign in again"),
    ACCOUNT_VERIFICATION_NOT_ALLOWED(ErrorType.FORBIDDEN,
            "An active account with a verified email is required for account verification"),
    ACCOUNT_PROOF_INVALID(ErrorType.INVALID,
            "Account proof is invalid for this account, email or purpose"),
    ACCOUNT_PROOF_EXPIRED(ErrorType.EXPIRED,
            "Account proof expired or is unavailable; verify your email again"),
    ACCOUNT_PROOF_ALREADY_USED(ErrorType.CONFLICT,
            "Account proof was already used; verify your email again before another attempt"),
    AUTHENTICATION_REVOKED(ErrorType.UNAUTHENTICATED,
            "Authentication was revoked; sign in again"),
    PASSWORD_CHANGE_NOT_ALLOWED(ErrorType.FORBIDDEN,
            "Password change requires an existing password login; adding a password is not supported here"),
    PASSWORD_CHANGE_CONFLICT(ErrorType.CONFLICT,
            "Account state changed; sign in again and obtain a new verification proof before retrying"),
    PASSWORD_CHANGE_UNAVAILABLE(ErrorType.UNAVAILABLE,
            "Password change could not be confirmed; the proof is not restored. Clear local login state and sign in again to confirm the outcome before retrying"),
    ACCOUNT_DELETION_CONFLICT(ErrorType.CONFLICT,
            "Account state changed; if the account is still active, sign in and verify your email again before retrying"),
    ACCOUNT_DELETION_UNAVAILABLE(ErrorType.UNAVAILABLE,
            "Account deletion could not be confirmed; the proof is not restored. Clear local login state and do not retry or create a new account automatically");

    private final ErrorType type;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
