package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountLookup;
import com.daebangg.sayroam.auth.application.port.out.AccountVerificationStore;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class AccountVerificationService {
    private final AccountLookup accounts;
    private final AccountVerificationStore store;
    private final Optional<VerificationEmailSender> mailSender;
    private final SecureRandom random = new SecureRandom();

    public AccountVerificationService(AccountLookup accounts, AccountVerificationStore store,
                                      Optional<VerificationEmailSender> mailSender) {
        this.accounts = accounts;
        this.store = store;
        this.mailSender = mailSender;
    }

    public IssuedRequest request(long userId, long expectedAuthVersion, AccountVerificationPurpose purpose,
                                 String remoteAddress) {
        var subject = subject(userId, expectedAuthVersion, purpose);
        VerificationEmailSender sender = mailSender.orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_DELIVERY_DISABLED));
        String requestId = UUID.randomUUID().toString();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        var retryAfter = store.reserve(subject, remoteAddress, requestId, codeDigest(subject, requestId, code));
        if (!retryAfter.isZero()) {
            throw new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, retryAfter);
        }
        try {
            sender.sendAccountVerificationCode(subject.email(), code, purpose);
        } catch (AuthException deliveryFailure) {
            try {
                store.discard(subject, requestId);
            } catch (AuthException cleanupFailure) {
                cleanupFailure.addSuppressed(deliveryFailure);
                throw cleanupFailure;
            }
            throw deliveryFailure;
        }
        var active = store.activate(subject, requestId).orElseThrow(
                () -> new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED));
        return new IssuedRequest(requestId, purpose, active.expiresInSeconds(), active.resendAfterSeconds());
    }

    public ConfirmedRequest confirm(long userId, long expectedAuthVersion, AccountVerificationPurpose purpose,
                                    String requestId, String code) {
        var subject = subject(userId, expectedAuthVersion, purpose);
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String proof = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        var result = store.confirm(subject, requestId, codeDigest(subject, requestId, code),
                VerificationDigests.sha256(proof));
        return switch (result.status()) {
            case CONFIRMED -> new ConfirmedRequest(proof, purpose, result.proofExpiresInSeconds());
            case EXPIRED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_EXPIRED);
            case SUPERSEDED -> throw new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED);
            case INVALID_CODE -> throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID);
            case ATTEMPTS_EXCEEDED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
            case ALREADY_CONFIRMED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ALREADY_USED);
        };
    }

    /** 후속 민감 작업 서비스용. 목적은 클라이언트 값 대신 해당 작업의 상수로 전달해야 한다. */
    public VerifiedAction consumeProof(long userId, long expectedAuthVersion, AccountVerificationPurpose purpose,
                                       String proof) {
        var subject = subject(userId, expectedAuthVersion, purpose);
        if (proof == null || !proof.matches("^[A-Za-z0-9_-]{43}$")) {
            throw new AuthException(AuthErrorCode.ACCOUNT_PROOF_INVALID);
        }
        var result = store.consumeProof(subject, VerificationDigests.sha256(proof));
        return switch (result.status()) {
            case CONSUMED -> new VerifiedAction(subject.email(), subject.authVersion(), result.verifiedAt());
            case EXPIRED -> throw new AuthException(AuthErrorCode.ACCOUNT_PROOF_EXPIRED);
            case INVALID -> throw new AuthException(AuthErrorCode.ACCOUNT_PROOF_INVALID);
            case ALREADY_USED -> throw new AuthException(AuthErrorCode.ACCOUNT_PROOF_ALREADY_USED);
        };
    }

    private AccountVerificationStore.Subject subject(long userId, long expectedAuthVersion,
                                                      AccountVerificationPurpose purpose) {
        Objects.requireNonNull(purpose, "purpose");
        var account = accounts.findActiveVerifiedAccount(userId).orElseThrow(
                () -> new AuthException(AuthErrorCode.ACCOUNT_VERIFICATION_NOT_ALLOWED));
        if (account.authVersion() != expectedAuthVersion) {
            throw new AuthException(AuthErrorCode.AUTHENTICATION_REVOKED);
        }
        return new AccountVerificationStore.Subject(userId, EmailAddress.normalize(account.email()), purpose,
                account.authVersion());
    }

    private String codeDigest(AccountVerificationStore.Subject subject, String requestId, String code) {
        return VerificationDigests.sha256(subject.userId() + ":" + subject.email() + ":"
                + subject.purpose().name() + ":" + subject.authVersion() + ":" + requestId + ":" + code);
    }

    public record VerifiedAction(String email, long authVersion, Instant verifiedAt) {
        @Override
        public String toString() {
            return "VerifiedAction[redacted]";
        }
    }

    public record IssuedRequest(String verificationId, AccountVerificationPurpose purpose,
                                long expiresInSeconds, long resendAfterSeconds) {
    }

    public record ConfirmedRequest(String accountProof, AccountVerificationPurpose purpose, long expiresInSeconds) {
        @Override
        public String toString() {
            return "ConfirmedRequest[accountProof=<redacted>, purpose=" + purpose
                    + ", expiresInSeconds=" + expiresInSeconds + "]";
        }
    }
}
