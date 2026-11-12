package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.port.out.OAuthTransactionStore;
import com.daebangg.sayroam.user.domain.SocialProvider;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisOAuthTransactionStore implements OAuthTransactionStore {
    private final RedisOAuthState state;

    public RedisOAuthTransactionStore(RedisOAuthState state) { this.state = state; }

    @Override
    public boolean save(String digest, Transaction transaction, Duration ttl) {
        return state.save("transaction", digest, transaction.provider().name(), transaction.browserBindingDigest(), transaction, ttl);
    }

    @Override
    public Optional<Transaction> consume(String digest, SocialProvider provider, String browserDigest) {
        return state.consume("transaction", digest, provider.name(), browserDigest, Transaction.class);
    }
}
