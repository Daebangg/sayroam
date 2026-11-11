package com.daebangg.sayroam.user.application.dto;

import com.daebangg.sayroam.user.domain.SocialProvider;
import java.util.List;

public record UserLoginMethodsResult(boolean passwordEnabled, List<SocialProvider> socialProviders) {
    public UserLoginMethodsResult {
        socialProviders = List.copyOf(socialProviders);
    }
}
