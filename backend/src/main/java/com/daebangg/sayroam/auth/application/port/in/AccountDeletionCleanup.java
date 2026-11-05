package com.daebangg.sayroam.auth.application.port.in;

/** 회원 잠금·상태 확인을 마친 탈퇴 트랜잭션에서 호출하는 계정 소유 데이터 정리 작업. */
public interface AccountDeletionCleanup {
    void deleteOwnedData(long userId);
}
