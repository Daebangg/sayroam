package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.domain.AuthenticatedAccount;

import java.util.Optional;

public interface AccountLookup {
    Optional<Credentials> findActiveCredentials(String normalizedEmail);

    Optional<AuthenticatedAccount> findActiveAccount(long userId);

    record Credentials(long userId, String passwordHash) {
        @Override
        public String toString() {
            return "Credentials[redacted]";
        }
    }
}
