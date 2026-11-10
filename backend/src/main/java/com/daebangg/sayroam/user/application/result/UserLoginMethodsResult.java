package com.daebangg.sayroam.user.application.result;

import com.daebangg.sayroam.user.domain.type.SocialProvider;
import java.util.List;

public record UserLoginMethodsResult(boolean passwordEnabled, List<SocialProvider> socialProviders) {
    public UserLoginMethodsResult {
        socialProviders = List.copyOf(socialProviders);
    }
}
