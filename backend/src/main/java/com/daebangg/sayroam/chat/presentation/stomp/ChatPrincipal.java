package com.daebangg.sayroam.chat.presentation.stomp;

import java.security.Principal;
import java.time.Instant;

public record ChatPrincipal(Long userId, Instant expiresAt) implements Principal {
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
