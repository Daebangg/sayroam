package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.user.domain.type.SocialProvider;

import java.time.Instant;
import java.util.Optional;

public interface SocialAccounts {
    Optional<Account> findActive(SocialProvider provider, String providerUserId);

    boolean emailExists(String normalizedEmail);

    /** User와 UserSocialAccount를 같은 트랜잭션에서 생성한다. 비밀번호 자격 정보는 만들지 않는다. */
    Account register(SocialIdentity identity, String normalizedEmail, String nickname, Instant emailVerifiedAt);

    record Account(long userId, long authVersion) { }
}
