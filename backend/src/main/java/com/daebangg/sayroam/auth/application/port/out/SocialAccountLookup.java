package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.dto.SocialAccountResult;
import com.daebangg.sayroam.user.domain.SocialProvider;

import java.util.Optional;

public interface SocialAccountLookup {
    Optional<SocialAccountResult> findActive(SocialProvider provider, String providerUserId);

    boolean emailExists(String normalizedEmail);

    Optional<SocialAccountResult> findActiveByEmail(String normalizedEmail);
}
