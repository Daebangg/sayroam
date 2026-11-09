package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.LoginAttemptStore;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.infrastructure.config.LoginAttemptProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

@Repository
public class RedisLoginAttemptStore implements LoginAttemptStore {
    private static final String PREFIX = "sayroam:auth:{login-attempts}:";
    private static final DefaultRedisScript<List> SCRIPT = new DefaultRedisScript<>();
    static {
        SCRIPT.setLocation(new ClassPathResource("redis/login-attempt.lua"));
        SCRIPT.setResultType(List.class);
    }
    private final StringRedisTemplate redis;
    private final LoginAttemptProperties policy;

    public RedisLoginAttemptStore(StringRedisTemplate redis, LoginAttemptProperties policy) {
        this.redis = redis;
        this.policy = policy;
    }

    @Override
    public Evaluation evaluate(String normalizedEmail, String remoteAddress, Outcome outcome) {
        try {
            // 같은 slot에서 두 제한을 원자적으로 관리하며 이메일·IP 원문은 저장하지 않는다.
            List<?> result = redis.execute(SCRIPT, List.of(PREFIX + "email:" + VerificationDigests.sha256(normalizedEmail),
                            PREFIX + "ip-wide:" + VerificationDigests.sha256(remoteAddress)),
                    outcome.name(), Integer.toString(policy.maxFailures()),
                    Long.toString(policy.windowSeconds() * 1000L), Long.toString(policy.blockSeconds() * 1000L),
                    Integer.toString(policy.ipMaxFailures()));
            if (result == null || result.size() != 2 || !(result.get(0) instanceof Number wait)
                    || !(result.get(1) instanceof Number attempts) || wait.longValue() < 0
                    || attempts.longValue() < 0 || attempts.longValue() > policy.maxFailures()) {
                throw new IllegalStateException("Invalid login attempt result");
            }
            return new Evaluation(Duration.ofMillis(wait.longValue()), attempts.intValue());
        } catch (DataAccessException exception) {
            // 저장 결과가 불확실하면 검증·발급을 진행하거나 자동 재시도하지 않는다.
            throw new AuthException(AuthErrorCode.LOGIN_ATTEMPTS_UNAVAILABLE, exception);
        }
    }
}
