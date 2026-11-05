package com.daebangg.sayroam.auth.application.port.out;

public interface PasswordChange {
    /**
     * 회원을 잠그고 현재 이메일·상태·인증 버전·기존 비밀번호 등록 여부를 다시 확인한다.
     * 해시·변경 시각 저장과 인증 버전 증가를 하나의 트랜잭션으로 커밋한 뒤 반환한다.
     * 이미 소비한 본인 확인 증명은 실패해도 복원하지 않는다.
     */
    void change(long userId, String verifiedEmail, long expectedAuthVersion, String passwordHash);
}
