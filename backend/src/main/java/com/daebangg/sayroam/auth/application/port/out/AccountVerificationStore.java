package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public interface AccountVerificationStore {
    /** 제한 확인·횟수 증가·같은 회원/이메일/목적의 코드 및 증명 교체를 원자 처리한다. */
    Duration reserve(Subject subject, String remoteAddress, String requestId, String codeDigest);

    /** SMTP 성공 후 같은 대기 요청만 활성화한다. */
    Optional<ActiveRequest> activate(Subject subject, String requestId);

    /** SMTP 실패 시 같은 대기 요청만 삭제한다. 발송 제한은 돌려주지 않는다. */
    void discard(Subject subject, String requestId);

    /** 바인딩·코드·시도 한도를 확인하고 코드 소비와 증명 생성을 원자 처리한다. */
    ConfirmationResult confirm(Subject subject, String requestId, String codeDigest, String proofDigest);

    /** 바인딩·만료·증명을 확인하고 한 번만 소비한다. 후속 DB 실패 시 복원하지 않는다. */
    ProofConsumption consumeProof(Subject subject, String proofDigest);

    record Subject(long userId, String email, AccountVerificationPurpose purpose, long authVersion) {
        @Override
        public String toString() {
            return "Subject[redacted]";
        }
    }

    record ActiveRequest(long expiresInSeconds, long resendAfterSeconds) {
    }

    enum ConfirmationStatus {
        CONFIRMED, EXPIRED, SUPERSEDED, INVALID_CODE, ATTEMPTS_EXCEEDED, ALREADY_CONFIRMED
    }

    record ConfirmationResult(ConfirmationStatus status, long proofExpiresInSeconds) {
    }

    enum ProofStatus {
        CONSUMED, EXPIRED, INVALID, ALREADY_USED
    }

    /** verifiedAt은 CONSUMED일 때만 존재한다. */
    record ProofConsumption(ProofStatus status, Instant verifiedAt) {
    }
}
