package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;
import com.daebangg.sayroam.user.domain.SocialProvider;

import java.util.Optional;

public interface AccountLookup {
    /** DB 이메일 UNIQUE와 동일한 전체 계정을 확인한다. 소셜 전용·탈퇴 계정도 포함한다. */
    boolean emailExists(String normalizedEmail);

    Optional<Credentials> findActiveCredentials(String normalizedEmail);

    Optional<AuthenticatedAccount> findActiveAccount(long userId);

    /** 민감 작업은 활성 회원의 현재 인증된 대표 이메일로만 진행한다. */
    Optional<VerifiedAccount> findActiveVerifiedAccount(long userId);

    Optional<SocialAccountResult> findActiveSocialAccount(SocialProvider provider, String providerUserId);

    Optional<SocialAccountResult> findActiveSocialAccountByEmail(String normalizedEmail);

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
