package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.infrastructure.config.EmailVerificationProperties;
import com.daebangg.sayroam.auth.application.port.out.EmailVerificationStore;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class RedisEmailVerificationStore implements EmailVerificationStore {
    // 같은 hash tag를 사용해 다중 키 Lua 작업이 같은 Redis slot에 속하게 한다.
    private static final String PREFIX = "sayroam:auth:{email-verification}:";
    private static final DefaultRedisScript<List> RESERVE = script("email-verification-reserve.lua");
    private static final DefaultRedisScript<List> ACTIVATE = script("email-verification-activate.lua");
    private static final DefaultRedisScript<List> CONFIRM = script("email-verification-confirm.lua");
    private static final DefaultRedisScript<List> CONSUME_PROOF = script("signup-proof-consume.lua");
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
    public Duration reserve(String email, String remoteAddress, String requestId, String codeDigest) {
        String emailHash = VerificationDigests.sha256(email);
        String ipHash = VerificationDigests.sha256(remoteAddress);
        List<?> result = execute(RESERVE,
                List.of(stateKey(emailHash), cooldownKey(emailHash), PREFIX + "email-limit:" + emailHash,
                        PREFIX + "ip-limit:" + ipHash, proofKey(emailHash)),
                requestId, codeDigest, millis(policy.codeTtlSeconds()), millis(policy.resendCooldownSeconds()),
                millis(policy.rateWindowSeconds()), Integer.toString(policy.maxRequestsPerEmail()),
                Integer.toString(policy.maxRequestsPerIp()));
        long outcome = number(result, 0);
        if (outcome == 0) {
            return Duration.ZERO;
        }
        if (outcome == 1 && number(result, 1) > 0) {
            return Duration.ofMillis(number(result, 1));
        }
        throw new IllegalStateException("Unexpected verification reservation result");
    }

    @Override
    public Optional<ActiveRequest> activate(String email, String requestId) {
        String emailHash = VerificationDigests.sha256(email);
        List<?> result = execute(ACTIVATE, List.of(stateKey(emailHash), cooldownKey(emailHash)), requestId);
        long ttl = number(result, 0);
        if (ttl <= 0) {
            return Optional.empty();
        }
        return Optional.of(new ActiveRequest(seconds(ttl), seconds(number(result, 1))));
    }

    @Override
    public ConfirmationResult confirm(String email, String requestId, String codeDigest, String signupProofDigest) {
        String emailHash = VerificationDigests.sha256(email);
        List<?> result = execute(CONFIRM, List.of(stateKey(emailHash), proofKey(emailHash)),
                requestId, codeDigest, signupProofDigest, Integer.toString(policy.maxVerificationAttempts()),
                millis(policy.signupProofTtlSeconds()));
        ConfirmationStatus status = switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ConfirmationStatus.CONFIRMED;
            case 1 -> ConfirmationStatus.EXPIRED;
            case 2 -> ConfirmationStatus.SUPERSEDED;
            case 3 -> ConfirmationStatus.INVALID_CODE;
            case 4 -> ConfirmationStatus.ATTEMPTS_EXCEEDED;
            case 5 -> ConfirmationStatus.ALREADY_CONFIRMED;
            default -> throw new IllegalStateException("Unexpected verification confirmation result");
        };
        long ttl = number(result, 1);
        if (status == ConfirmationStatus.CONFIRMED && ttl <= 0) {
            throw new IllegalStateException("Invalid signup proof lifetime");
        }
        return new ConfirmationResult(status, status == ConfirmationStatus.CONFIRMED ? seconds(ttl) : 0);
    }

    @Override
    public ProofConsumption consumeSignupProof(String email, String signupProofDigest) {
        List<?> result = execute(CONSUME_PROOF,
                List.of(proofKey(VerificationDigests.sha256(email))), signupProofDigest);
        ProofStatus status = switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ProofStatus.CONSUMED;
            case 1 -> ProofStatus.EXPIRED;
            case 2 -> ProofStatus.INVALID;
            case 3 -> ProofStatus.ALREADY_USED;
            default -> throw new IllegalStateException("Unexpected signup proof consumption result");
        };
        long verifiedAt = number(result, 1);
        if (status == ProofStatus.CONSUMED && verifiedAt <= 0) {
            throw new IllegalStateException("Invalid email verification timestamp");
        }
        return new ProofConsumption(status,
                status == ProofStatus.CONSUMED ? Instant.ofEpochMilli(verifiedAt) : null);
    }

    @Override
    public void discard(String email, String requestId) {
        execute(DISCARD, List.of(stateKey(VerificationDigests.sha256(email))), requestId);
    }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            T result = redis.execute(script, keys, (Object[]) args);
            if (result == null) {
                throw new IllegalStateException("Missing verification script result");
            }
            return result;
        } catch (DataAccessException exception) {
            // 이메일·IP·코드·Redis 접속 비밀값은 로그에 기록하지 않는다.
            log.warn("Email verification Redis operation failed: {}", exception.getClass().getSimpleName());
            throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE, exception);
        }
    }

    private static long number(List<?> result, int index) {
        if (result.size() != 2 || !(result.get(index) instanceof Number number)) {
            throw new IllegalStateException("Unexpected verification script result");
        }
        return number.longValue();
    }

    private static String stateKey(String emailHash) {
        return PREFIX + "state:" + emailHash;
    }

    private static String proofKey(String emailHash) {
        return PREFIX + "proof:" + emailHash;
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
        DefaultRedisScript<List> script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("redis/" + fileName));
        script.setResultType(List.class);
        return script;
    }
}
