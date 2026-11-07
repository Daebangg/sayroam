package com.daebangg.sayroam.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

/** 가입·로그인 중 계정 관리와 분리한 비밀번호 분실 복구 상태. */
public interface PasswordResetStore {
    Duration reserve(Subject subject, String remoteAddress, String requestId, String codeDigest);
    Optional<ActiveRequest> activate(Subject subject, String requestId);
    void discard(Subject subject, String requestId);
    ConfirmationResult confirm(Subject subject, String requestId, String codeDigest, String proofDigest);
    ProofStatus consumeProof(Subject subject, String proofDigest);

    /** 비밀번호 로그인 대상이 없으면 userId=0. 공개 응답과 발송 경로는 동일하다. */
    record Subject(long userId, String email, long authVersion) {
        @Override public String toString() { return "Subject[redacted]"; }
    }

    record ActiveRequest(long expiresInSeconds, long resendAfterSeconds) {}
    enum ConfirmationStatus { CONFIRMED, EXPIRED, SUPERSEDED, INVALID_CODE, ATTEMPTS_EXCEEDED, ALREADY_CONFIRMED }
    record ConfirmationResult(ConfirmationStatus status, long proofExpiresInSeconds) {}
    enum ProofStatus { CONSUMED, EXPIRED, INVALID, ALREADY_USED }
}
