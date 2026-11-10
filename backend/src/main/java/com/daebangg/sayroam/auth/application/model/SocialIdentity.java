package com.daebangg.sayroam.auth.application.model;

import com.daebangg.sayroam.user.domain.type.SocialProvider;

import java.util.Objects;

/** 공급자 인증 결과. emailVerified는 공급자별 서비스 이메일 수락 정책이며 회원 식별자는 provider + providerUserId다. */
public record SocialIdentity(SocialProvider provider, String providerUserId, String email,
                             boolean emailVerified, String displayName, String profileImageUrl) {
    public SocialIdentity {
        Objects.requireNonNull(provider, "provider");
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("providerUserId is required");
        }
        if (emailVerified && (email == null || email.isBlank())) {
            throw new IllegalArgumentException("A verified email must be present");
        }
    }

    @Override
    public String toString() {
        return "SocialIdentity[redacted]";
    }
}
