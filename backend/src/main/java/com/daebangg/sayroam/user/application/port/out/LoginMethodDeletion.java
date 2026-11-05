package com.daebangg.sayroam.user.application.port.out;

public interface LoginMethodDeletion {
    /** 기존 탈퇴 트랜잭션에 참여해 해당 회원의 비밀번호·로컬 소셜 연결을 삭제한다. */
    void deleteForUser(long userId);
}
