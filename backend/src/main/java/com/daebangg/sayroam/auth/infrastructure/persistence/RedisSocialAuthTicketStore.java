package com.daebangg.sayroam.auth.infrastructure.persistence;

import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
public class RedisSocialAuthTicketStore implements SocialAuthTicketStore {
    private final RedisOAuthState state;

    public RedisSocialAuthTicketStore(RedisOAuthState state) { this.state = state; }

    @Override public boolean saveResult(String digest, String browser, Result result, Duration ttl) {
        return state.save("result", digest, "", browser, result, ttl);
    }
    @Override public Optional<Result> consumeResult(String digest, String browser) {
        return state.consume("result", digest, "", browser, Result.class);
    }
    @Override public boolean saveSignup(String digest, String browser, SocialIdentity identity, Duration ttl) {
        return state.save("signup", digest, "", browser, identity, ttl);
    }
    @Override public Optional<SocialIdentity> consumeSignup(String digest, String browser) {
        return state.consume("signup", digest, "", browser, SocialIdentity.class);
    }
}
