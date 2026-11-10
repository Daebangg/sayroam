package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.global.mail.EmailLanguage;
import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationStore store;
    private final Optional<VerificationEmailSender> mailSender;
    private final AccountLookup accounts;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationStore store, Optional<VerificationEmailSender> mailSender, AccountLookup accounts) {
        this.store = store;
        this.mailSender = mailSender;
        this.accounts = accounts;
    }

    public IssuedRequest request(String rawEmail, String remoteAddress, EmailLanguage language) {
        // 기능이 꺼져 있으면 Redis 예약·제한 소비 전에 실패한다.
        VerificationEmailSender sender = mailSender.orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_DELIVERY_DISABLED));
        String email = EmailAddress.normalize(rawEmail);
        if (accounts.emailExists(email)) throw new AuthException(AuthErrorCode.EMAIL_ALREADY_REGISTERED);
        String requestId = UUID.randomUUID().toString();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String digest = VerificationDigests.sha256(requestId + ":" + code);
        Duration retryAfter = store.reserve(email, remoteAddress, requestId, digest);
        if (!retryAfter.isZero()) {
            throw new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, retryAfter);
        }
        try {
            sender.sendVerificationCode(email, code, language);
        } catch (AuthException deliveryFailure) {
            try {
                store.discard(email, requestId);
            } catch (AuthException cleanupFailure) {
                // 정리 실패를 우선 전파하되 SMTP 원인도 내부 진단을 위해 보존한다.
                cleanupFailure.addSuppressed(deliveryFailure);
                throw cleanupFailure;
            }
            throw deliveryFailure;
        }
        EmailVerificationStore.ActiveRequest active = store.activate(email, requestId).orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED));
        return new IssuedRequest(requestId, active.expiresInSeconds(), active.resendAfterSeconds());
    }

    public ConfirmedRequest confirm(String rawEmail, String requestId, String code) {
        String email = EmailAddress.normalize(rawEmail);
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String signupProof = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        EmailVerificationStore.ConfirmationResult result = store.confirm(email, requestId, VerificationDigests.sha256(requestId + ":" + code),
                VerificationDigests.sha256(signupProof));
        return switch (result.status()) {
            case CONFIRMED -> new ConfirmedRequest(signupProof, result.proofExpiresInSeconds());
            case EXPIRED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_EXPIRED);
            case SUPERSEDED -> throw new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED);
            case INVALID_CODE -> throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID);
            case ATTEMPTS_EXCEEDED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
            case ALREADY_CONFIRMED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ALREADY_USED);
        };
    }

    public record ConfirmedRequest(String signupProof, long expiresInSeconds) {
        @Override
        public String toString() {
            return "ConfirmedRequest[signupProof=<redacted>, expiresInSeconds=" + expiresInSeconds + "]";
        }
    }

    public record IssuedRequest(String verificationId, long expiresInSeconds, long resendAfterSeconds) {
    }
}
