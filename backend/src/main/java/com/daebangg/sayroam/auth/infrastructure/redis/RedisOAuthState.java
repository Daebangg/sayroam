package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** OAuth의 짧은 상태만 저장한다. 결과 원문·토큰·이메일을 키 또는 로그에 넣지 않는다. */
@Component
public class RedisOAuthState {
    private static final DefaultRedisScript<Long> SAVE = new DefaultRedisScript<>();
    private static final DefaultRedisScript<String> CONSUME = new DefaultRedisScript<>();
    static {
        SAVE.setLocation(new ClassPathResource("redis/oauth-state-save.lua"));
        SAVE.setResultType(Long.class);
        CONSUME.setLocation(new ClassPathResource("redis/oauth-state-consume.lua"));
        CONSUME.setResultType(String.class);
    }
    private final StringRedisTemplate redis;
    private final JsonMapper json;

    public RedisOAuthState(StringRedisTemplate redis, JsonMapper json) {
        this.redis = redis;
        this.json = json;
    }

    public boolean save(String namespace, String digest, String provider, String browser, Object payload, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) throw new IllegalArgumentException("OAuth state TTL must be positive");
        try {
            Long result = redis.execute(SAVE, List.of(key(namespace, digest)), provider, browser,
                    json.writeValueAsString(payload), Long.toString(ttl.toMillis()));
            if (result == null) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
            return result == 1L;
        } catch (DataAccessException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, exception);
        }
    }

    public <T> Optional<T> consume(String namespace, String digest, String provider, String browser, Class<T> type) {
        try {
            String payload = redis.execute(CONSUME, List.of(key(namespace, digest)), provider, browser);
            return payload == null ? Optional.empty() : Optional.of(json.readValue(payload, type));
        } catch (DataAccessException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE, exception);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Invalid stored OAuth state");
        }
    }

    private String key(String namespace, String digest) {
        if (digest == null || !digest.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid OAuth state digest");
        return "sayroam:auth:oauth:" + namespace + ":{" + digest + "}";
    }
}
