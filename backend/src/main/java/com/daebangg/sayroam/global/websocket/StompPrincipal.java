package com.daebangg.sayroam.global.websocket;

import java.security.Principal;
import java.time.Instant;

public record StompPrincipal(Long userId, long authVersion, Instant expiresAt) implements Principal {
    @Override
    public String getName() {
        return userId.toString();
    }

    public void ensureNotExpired(Instant now) {
        if (now.isAfter(expiresAt) || now.equals(expiresAt)) {
            throw new StompAuthenticationException();
        }
    }
}
