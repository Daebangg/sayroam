package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;

import java.util.Optional;

public interface AccountLookup {
    Optional<Credentials> findActiveCredentials(String normalizedEmail);

    Optional<AuthenticatedAccount> findActiveAccount(long userId);

    /** 민감 작업은 활성 회원의 현재 인증된 대표 이메일로만 진행한다. */
    Optional<VerifiedAccount> findActiveVerifiedAccount(long userId);

    record VerifiedAccount(String email, long authVersion) {
        @Override
        public String toString() {
            return "VerifiedAccount[redacted]";
        }
    }

    record Credentials(long userId, String passwordHash, long authVersion) {
        @Override
        public String toString() {
            return "Credentials[redacted]";
        }
    }
}
