package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.*;
import com.daebangg.sayroam.auth.domain.EmailAddress;
import com.daebangg.sayroam.auth.domain.PasswordPolicy;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetService {
    private final AccountLookup accounts;
    private final PasswordResetStore store;
    private final Optional<VerificationEmailSender> mailSender;
    private final PasswordHasher passwords;
    private final PasswordChange passwordChange;
    private final SecureRandom random = new SecureRandom();

    public PasswordResetService(AccountLookup accounts, PasswordResetStore store,
                                Optional<VerificationEmailSender> mailSender, PasswordHasher passwords,
                                PasswordChange passwordChange) {
        this.accounts = accounts;
        this.store = store;
        this.mailSender = mailSender;
        this.passwords = passwords;
        this.passwordChange = passwordChange;
    }

    public IssuedRequest request(String rawEmail, String remoteAddress) {
        var sender = mailSender.orElseThrow(() -> new AuthException(AuthErrorCode.EMAIL_DELIVERY_DISABLED));
        var subject = subject(rawEmail);
        String requestId = UUID.randomUUID().toString();
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        var retryAfter = store.reserve(subject, remoteAddress, requestId, codeDigest(subject, requestId, code));
        if (!retryAfter.isZero()) throw new AuthException(AuthErrorCode.EMAIL_RATE_LIMITED, retryAfter);
        try {
            // 같은 SMTP·오류 경로를 거쳐 계정 유무·비밀번호 등록 여부의 조기 반환을 없앤다.
            sender.sendPasswordResetCode(subject.email(), code);
        } catch (AuthException deliveryFailure) {
            try {
                store.discard(subject, requestId);
            } catch (AuthException cleanupFailure) {
                cleanupFailure.addSuppressed(deliveryFailure);
                throw cleanupFailure;
            }
            throw deliveryFailure;
        }
        var active = store.activate(subject, requestId).orElseThrow(() -> new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED));
        return new IssuedRequest(requestId, active.expiresInSeconds(), active.resendAfterSeconds());
    }

    public ConfirmedRequest confirm(String rawEmail, String requestId, String code) {
        var subject = subject(rawEmail);
        byte[] secret = new byte[32];
        random.nextBytes(secret);
        String proof = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        var result = store.confirm(subject, requestId, codeDigest(subject, requestId, code), VerificationDigests.sha256(proof));
        return switch (result.status()) {
            case CONFIRMED -> new ConfirmedRequest(proof, result.proofExpiresInSeconds());
            case EXPIRED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_EXPIRED);
            case SUPERSEDED -> throw new AuthException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED);
            case INVALID_CODE -> throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID);
            case ATTEMPTS_EXCEEDED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ATTEMPTS_EXCEEDED);
            case ALREADY_CONFIRMED -> throw new AuthException(AuthErrorCode.EMAIL_CODE_ALREADY_USED);
        };
    }

    public void reset(String rawEmail, String resetProof, String newPassword) {
        if (!PasswordPolicy.isValid(newPassword)) throw new AuthException(AuthErrorCode.PASSWORD_POLICY_VIOLATION);
        if (resetProof == null || !resetProof.matches("^[A-Za-z0-9_-]{43}$")) throw new AuthException(AuthErrorCode.PASSWORD_RESET_PROOF_INVALID);
        var subject = subject(rawEmail);
        // 해시 실패로 증명이 소비되지 않게 하며 비밀번호 미등록 계정도 같은 해싱 경로를 거친다.
        String hash = passwords.hash(newPassword);
        switch (store.consumeProof(subject, VerificationDigests.sha256(resetProof))) {
            case EXPIRED -> throw new AuthException(AuthErrorCode.PASSWORD_RESET_PROOF_EXPIRED);
            case INVALID -> throw new AuthException(AuthErrorCode.PASSWORD_RESET_PROOF_INVALID);
            case ALREADY_USED -> throw new AuthException(AuthErrorCode.PASSWORD_RESET_PROOF_ALREADY_USED);
            case CONSUMED -> { }
        }
        // 소셜 전용·미가입·탈퇴·미인증 계정에는 비밀번호를 생성하지 않는다.
        if (subject.userId() == 0) throw new AuthException(AuthErrorCode.PASSWORD_RESET_PROOF_INVALID);
        try {
            passwordChange.change(subject.userId(), subject.email(), subject.authVersion(), hash);
        } catch (AuthException exception) {
            throw switch (exception.getErrorCode()) {
                case PASSWORD_CHANGE_NOT_ALLOWED, PASSWORD_CHANGE_CONFLICT -> new AuthException(AuthErrorCode.PASSWORD_RESET_CONFLICT, exception);
                case PASSWORD_CHANGE_UNAVAILABLE -> new AuthException(AuthErrorCode.PASSWORD_RESET_UNAVAILABLE, exception);
                default -> exception;
            };
        }
        // 기존 DB 트랜잭션이 비밀번호·변경 시각·authVersion을 함께 커밋해 모든 이전 로그인을 차단한다.
    }

    private PasswordResetStore.Subject subject(String rawEmail) {
        String email = EmailAddress.normalize(rawEmail);
        return accounts.findActiveCredentials(email)
                .map(account -> new PasswordResetStore.Subject(account.userId(), email, account.authVersion()))
                .orElseGet(() -> new PasswordResetStore.Subject(0, email, 0));
    }

    private String codeDigest(PasswordResetStore.Subject subject, String requestId, String code) {
        return VerificationDigests.sha256(subject.userId() + ":" + subject.email() + ":PASSWORD_RESET:"
                + subject.authVersion() + ":" + requestId + ":" + code);
    }

    public record IssuedRequest(String verificationId, long expiresInSeconds, long resendAfterSeconds) {}
    public record ConfirmedRequest(String resetProof, long expiresInSeconds) {
        @Override public String toString() { return "ConfirmedRequest[resetProof=<redacted>, expiresInSeconds=" + expiresInSeconds + "]"; }
    }
}
