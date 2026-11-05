package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.RefreshTokenStore;
import com.daebangg.sayroam.auth.infrastructure.config.RefreshTokenProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final DefaultRedisScript<Long> CREATE = new DefaultRedisScript<>();
    private static final DefaultRedisScript<List> USE = new DefaultRedisScript<>();

    static {
        CREATE.setLocation(new ClassPathResource("redis/refresh-token-create.lua"));
        CREATE.setResultType(Long.class);
        USE.setLocation(new ClassPathResource("redis/refresh-token-use.lua"));
        USE.setResultType(List.class);
    }

    private final StringRedisTemplate redis;
    private final RefreshTokenProperties policy;

    public RedisRefreshTokenStore(StringRedisTemplate redis, RefreshTokenProperties policy) {
        this.redis = redis;
        this.policy = policy;
    }

    @Override
    public long create(String sessionId, long userId, String tokenDigest, long authVersion) {
        if (authVersion < 0) {
            throw new IllegalArgumentException("Authentication version must be nonnegative");
        }
        long ttl = execute(CREATE, sessionId, Long.toString(userId), tokenDigest,
                Long.toString(policy.ttlSeconds() * 1000L), Integer.toString(policy.maxRotations()),
                Long.toString(authVersion));
        if (ttl < 1000) {
            throw new IllegalStateException("Refresh session could not be created");
        }
        return ttl / 1000;
    }

    @Override
    public SessionResult inspect(String sessionId, String tokenDigest) {
        return use(sessionId, "inspect", tokenDigest, "");
    }

    @Override
    public SessionResult rotate(String sessionId, String currentDigest, String nextDigest) {
        return use(sessionId, "rotate", currentDigest, nextDigest);
    }

    @Override
    public void revoke(String sessionId, String tokenDigest) {
        use(sessionId, "revoke", tokenDigest, "");
    }

    private SessionResult use(String sessionId, String mode, String digest, String nextDigest) {
        List<?> result = execute(USE, sessionId, mode, digest, nextDigest);
        if (result.size() != 4 || !(result.get(0) instanceof Number status)
                || !(result.get(2) instanceof Number ttl)) {
            throw new IllegalStateException("Unexpected refresh session result");
        }
        return switch (status.intValue()) {
            case 0 -> {
                if (!(result.get(1) instanceof String userId) || ttl.longValue() < 1000
                        || !(result.get(3) instanceof String authVersion)) {
                    throw new IllegalStateException("Invalid active refresh session");
                }
                long id = Long.parseLong(userId);
                long version = Long.parseLong(authVersion);
                if (id <= 0 || version < 0) {
                    throw new IllegalStateException("Invalid refresh session account");
                }
                yield new SessionResult(Status.ACTIVE, id, ttl.longValue() / 1000, version);
            }
            case 1 -> new SessionResult(Status.INVALID, 0, 0, 0);
            case 2 -> new SessionResult(Status.REUSED, 0, 0, 0);
            default -> throw new IllegalStateException("Invalid refresh session state");
        };
    }

    private <T> T execute(DefaultRedisScript<T> script, String sessionId, String... arguments) {
        try {
            // 한 로그인만 같은 Redis slot을 사용한다. 다른 로그인은 독립적으로 유지한다.
            T result = redis.execute(script, List.of("sayroam:auth:refresh:{" + sessionId + "}"),
                    (Object[]) arguments);
            if (result == null) {
                throw new IllegalStateException("Missing refresh session result");
            }
            return result;
        } catch (DataAccessException exception) {
            // 원문 토큰·해시·접속 정보는 로그에 기록하지 않는다. 불확실한 작업을 자동 재시도하지 않는다.
            throw new AuthException(AuthErrorCode.AUTHENTICATION_UNAVAILABLE, exception);
        }
    }
}
