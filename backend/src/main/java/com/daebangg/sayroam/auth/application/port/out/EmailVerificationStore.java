package com.daebangg.sayroam.auth.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface EmailVerificationStore {
    /**
     * 제한 확인·횟수 증가·기존 코드 교체를 원자 처리한다. 허용되면 ZERO, 제한이면 남은 대기시간.
     * @throws com.daebangg.sayroam.auth.application.error.AuthException 저장소 기술 오류가 발생한 경우
     */
    Duration reserve(String email, String remoteAddress, String requestId, String codeDigest);

    /** SMTP 성공 후 해당 요청만 활성화한다. 만료·교체된 요청은 empty이며 되살리지 않는다. */
    Optional<ActiveRequest> activate(String email, String requestId);

    /** SMTP 실패 시 해당 대기 요청만 삭제한다. 발송 제한은 되돌리지 않는다. */
    void discard(String email, String requestId);

    /** 코드 확인·시도 횟수 처리·코드 소비·이메일에 바인딩된 가입 증명 저장을 원자 처리한다. */
    ConfirmationResult confirm(String email, String requestId, String codeDigest, String signupProofDigest);

    enum ConfirmationStatus {
        CONFIRMED, EXPIRED, SUPERSEDED, INVALID_CODE, ATTEMPTS_EXCEEDED, ALREADY_CONFIRMED
    }

    record ConfirmationResult(ConfirmationStatus status, long proofExpiresInSeconds) {
    }

    record ActiveRequest(long expiresInSeconds, long resendAfterSeconds) {
    }
}
