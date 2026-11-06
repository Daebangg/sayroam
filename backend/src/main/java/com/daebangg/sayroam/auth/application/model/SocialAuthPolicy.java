package com.daebangg.sayroam.auth.application.model;

import java.time.Duration;
import java.util.Set;

public record SocialAuthPolicy(Duration transactionTtl, Duration resultTtl, Duration signupTtl,
                               Set<String> allowedReturnPaths) {
    public SocialAuthPolicy {
        allowedReturnPaths = Set.copyOf(allowedReturnPaths);
        if (allowedReturnPaths.isEmpty() || allowedReturnPaths.stream().anyMatch(path ->
                !path.matches("/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]*"))) {
            throw new IllegalArgumentException("OAuth return paths must be explicit local paths without query or fragment");
        }
    }
}
