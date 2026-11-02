package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationStore store;
    private final Optional<VerificationEmailSender> mailSender;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationStore store, Optional<VerificationEmailSender> mailSender) {
        this.store = store;
        this.mailSender = mailSender;
    }

    public IssuedRequest request(String rawEmail, String remoteAddress) {
        // 기능이 꺼져 있으면 Redis 예약·제한 소비 전에 실패한다.
        VerificationEmailSender sender = mailSender.orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_DELIVERY_DISABLED));
        String email = EmailAddress.normalize(rawEmail);
        String requestId = UUID.randomUUID().toString();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String digest = VerificationDigests.sha256(requestId + ":" + code);
        var retryAfter = store.reserve(email, remoteAddress, requestId, digest);
        if (!retryAfter.isZero()) {
            throw new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, retryAfter);
        }
        try {
            sender.sendVerificationCode(email, code);
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
        var active = store.activate(email, requestId).orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED));
        return new IssuedRequest(requestId, active.expiresInSeconds(), active.resendAfterSeconds());
    }

    public record IssuedRequest(String verificationId, long expiresInSeconds, long resendAfterSeconds) {
    }
}
