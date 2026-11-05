package com.daebangg.sayroam.auth.application.port.out;

public interface AccountDeletion {
    /**
     * 회원을 잠그고 현재 이메일·활성 상태·인증 버전을 다시 확인한다.
     * 프로필 익명화·탈퇴 상태·인증 버전 증가·로그인 수단 및 개인 설정 정리를 한 트랜잭션으로 커밋한다.
     * 공유 기록의 회원 ID 참조를 보존하며, 이미 소비한 증명은 실패해도 복원하지 않는다.
     */
    void delete(long userId, String verifiedEmail, long expectedAuthVersion);
}
