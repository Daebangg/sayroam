package com.daebangg.sayroam.auth.application.model;

import java.time.Duration;
import java.util.Set;

public record SocialAuthPolicy(Duration transactionTtl, Duration resultTtl, Duration signupTtl,
                               Set<String> allowedReturnPaths, java.net.URI frontendOrigin) {
    public SocialAuthPolicy {
        allowedReturnPaths = Set.copyOf(allowedReturnPaths);
        if (allowedReturnPaths.isEmpty() || allowedReturnPaths.stream().anyMatch(path ->
                !path.matches("/(?:[A-Za-z0-9_-]+/)*[A-Za-z0-9_-]*"))) {
            throw new IllegalArgumentException("OAuth return paths must be explicit local paths without query or fragment");
        }
        if (frontendOrigin != null && (frontendOrigin.getHost() == null || frontendOrigin.getUserInfo() != null
                || frontendOrigin.getRawQuery() != null || frontendOrigin.getRawFragment() != null
                || !(frontendOrigin.getRawPath().isEmpty() || frontendOrigin.getRawPath().equals("/"))
                || !("https".equals(frontendOrigin.getScheme()) || ("http".equals(frontendOrigin.getScheme())
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(frontendOrigin.getHost()))))) {
            throw new IllegalArgumentException("OAuth frontend-origin requires a fixed HTTPS origin (HTTP loopback only)");
        }
    }
}
