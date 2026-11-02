package com.daebangg.sayroam.auth.application.port.out;

import java.time.Instant;

public interface AccountRegistration {
    /** 일반 회원과 비밀번호 해시를 함께 커밋한 뒤 회원 ID를 반환한다. */
    long register(String email, String nickname, String passwordHash, Instant emailVerifiedAt);
}
