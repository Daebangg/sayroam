package com.daebangg.sayroam.auth.application;

public interface EmailVerificationStore {
    /** 제한 확인·횟수 증가·기존 코드 교체를 원자적으로 수행한다. */
    void reserve(String email, String remoteAddress, String requestId, String codeDigest);

    /** SMTP 성공 후 해당 요청만 활성화한다. 만료·교체된 요청을 되살리지 않는다. */
    ActiveRequest activate(String email, String requestId);

    /** SMTP 실패 시 해당 대기 요청만 삭제한다. 발송 제한은 되돌리지 않는다. */
    void discard(String email, String requestId);

    record ActiveRequest(long expiresInSeconds, long resendAfterSeconds) {
    }
}
