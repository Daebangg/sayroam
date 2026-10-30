package com.daebangg.sayroam.auth.application;

import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.UUID;

@Service
public class EmailVerificationService {
    private final EmailVerificationStore store;
    private final ObjectProvider<VerificationEmailSender> mailSenderProvider;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService(EmailVerificationStore store,
                                    ObjectProvider<VerificationEmailSender> mailSenderProvider) {
        this.store = store;
        this.mailSenderProvider = mailSenderProvider;
    }

    public IssuedRequest request(String rawEmail, String remoteAddress) {
        VerificationEmailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            throw new EmailVerificationException(AuthErrorCode.EMAIL_DELIVERY_DISABLED);
        }
        String email = EmailAddress.normalize(rawEmail);
        String requestId = UUID.randomUUID().toString();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        String digest = VerificationDigests.sha256(requestId + ":" + code);
        store.reserve(email, remoteAddress, requestId, digest);
        try {
            sender.sendVerificationCode(email, code);
        } catch (EmailVerificationException exception) {
            // 정리 중 Redis 장애가 발생하면 저장소 상태를 확신할 수 없으므로 503을 우선한다.
            store.discard(email, requestId);
            throw exception;
        }
        var active = store.activate(email, requestId);
        return new IssuedRequest(requestId, active.expiresInSeconds(), active.resendAfterSeconds());
    }

    public record IssuedRequest(String verificationId, long expiresInSeconds, long resendAfterSeconds) {
    }
}
