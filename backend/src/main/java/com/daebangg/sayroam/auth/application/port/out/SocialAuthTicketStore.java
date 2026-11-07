package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;

import java.time.Duration;
import java.util.Optional;

/** 결과와 가입 대기를 별도 namespace에 저장한다. 바인딩 검사·소비는 원자 처리하며 실패 시 복원하지 않는다. */
public interface SocialAuthTicketStore {
    boolean saveResult(String digest, String browserDigest, Result result, Duration ttl);
    Optional<Result> consumeResult(String digest, String browserDigest);
    boolean saveSignup(String digest, String browserDigest, SocialIdentity identity, Duration ttl);
    Optional<SocialIdentity> consumeSignup(String digest, String browserDigest);

    enum Status { LOGIN_READY, SIGNUP_REQUIRED, FAILED }

    record Result(Status status, SocialIdentity identity, SocialAccounts.Account account, AuthErrorCode error) {
        @Override
        public String toString() { return "SocialAuthResult[redacted]"; }
    }
}
