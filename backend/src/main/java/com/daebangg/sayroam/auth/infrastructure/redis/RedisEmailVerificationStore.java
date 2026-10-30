package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.AuthErrorCode;
import com.daebangg.sayroam.auth.application.EmailVerificationException;
import com.daebangg.sayroam.auth.application.EmailVerificationProperties;
import com.daebangg.sayroam.auth.application.EmailVerificationStore;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;

@Slf4j
@Repository
public class RedisEmailVerificationStore implements EmailVerificationStore {
    // 같은 hash tag를 사용해 다중 키 Lua 작업이 같은 Redis slot에 속하게 한다.
    private static final String PREFIX = "sayroam:auth:{email-verification}:";
    private static final DefaultRedisScript<List> RESERVE = script("email-verification-reserve.lua");
    private static final DefaultRedisScript<List> ACTIVATE = script("email-verification-activate.lua");
    private static final DefaultRedisScript<Long> DISCARD = new DefaultRedisScript<>();

    static {
        DISCARD.setLocation(new ClassPathResource("redis/email-verification-discard.lua"));
        DISCARD.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final EmailVerificationProperties policy;

    public RedisEmailVerificationStore(StringRedisTemplate redis, EmailVerificationProperties policy) {
        this.redis = redis;
        this.policy = policy;
    }

    @Override
    public void reserve(String email, String remoteAddress, String requestId, String codeDigest) {
        String emailHash = VerificationDigests.sha256(email);
        String ipHash = VerificationDigests.sha256(remoteAddress);
        List<?> result = execute(RESERVE,
                List.of(stateKey(emailHash), cooldownKey(emailHash), PREFIX + "email-limit:" + emailHash,
                        PREFIX + "ip-limit:" + ipHash),
                requestId, codeDigest, millis(policy.codeTtlSeconds()), millis(policy.resendCooldownSeconds()),
                millis(policy.rateWindowSeconds()), Integer.toString(policy.maxRequestsPerEmail()),
                Integer.toString(policy.maxRequestsPerIp()));
        if (number(result, 0) != 0) {
            throw new EmailVerificationException(AuthErrorCode.EMAIL_RATE_LIMITED, seconds(number(result, 1)));
        }
    }

    @Override
    public ActiveRequest activate(String email, String requestId) {
        String emailHash = VerificationDigests.sha256(email);
        List<?> result = execute(ACTIVATE, List.of(stateKey(emailHash), cooldownKey(emailHash)), requestId);
        long ttl = number(result, 0);
        if (ttl <= 0) {
            throw new EmailVerificationException(AuthErrorCode.EMAIL_REQUEST_SUPERSEDED);
        }
        return new ActiveRequest(seconds(ttl), seconds(number(result, 1)));
    }

    @Override
    public void discard(String email, String requestId) {
        execute(DISCARD, List.of(stateKey(VerificationDigests.sha256(email))), requestId);
    }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            T result = redis.execute(script, keys, (Object[]) args);
            if (result == null) {
                throw new EmailVerificationException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE);
            }
            return result;
        } catch (DataAccessException exception) {
            // 이메일·IP·코드·Redis 접속 비밀값은 로그에 기록하지 않는다.
            log.warn("Email verification Redis operation failed: {}", exception.getClass().getSimpleName());
            throw new EmailVerificationException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE);
        }
    }

    private static long number(List<?> result, int index) {
        if (result.size() != 2 || !(result.get(index) instanceof Number number)) {
            throw new EmailVerificationException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE);
        }
        return number.longValue();
    }

    private static String stateKey(String emailHash) {
        return PREFIX + "state:" + emailHash;
    }

    private static String cooldownKey(String emailHash) {
        return PREFIX + "cooldown:" + emailHash;
    }

    private static String millis(int seconds) {
        return Long.toString(seconds * 1000L);
    }

    private static long seconds(long millis) {
        return Math.max(0, (millis + 999) / 1000);
    }

    private static DefaultRedisScript<List> script(String fileName) {
        var script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("redis/" + fileName));
        script.setResultType(List.class);
        return script;
    }
}
