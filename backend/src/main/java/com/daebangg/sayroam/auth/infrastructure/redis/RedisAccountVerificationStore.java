package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.AccountVerificationStore;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.infrastructure.config.AccountVerificationProperties;
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
public class RedisAccountVerificationStore implements AccountVerificationStore {
    // 가입용 namespace와 분리한다. 회원/IP 합산 제한도 Lua에서 처리하도록 같은 slot을 사용한다.
    private static final String PREFIX = "sayroam:auth:{account-verification}:";
    private static final DefaultRedisScript<List> RESERVE = script("account-verification-reserve.lua");
    // 활성화/폐기는 코드·증명 형식에 의존하지 않아 기존 요청 ID 기반 스크립트를 공유한다.
    private static final DefaultRedisScript<List> ACTIVATE = script("email-verification-activate.lua");
    private static final DefaultRedisScript<List> CONFIRM = script("account-verification-confirm.lua");
    private static final DefaultRedisScript<List> CONSUME = script("account-proof-consume.lua");
    private static final DefaultRedisScript<Long> DISCARD = new DefaultRedisScript<>();

    static {
        DISCARD.setLocation(new ClassPathResource("redis/email-verification-discard.lua"));
        DISCARD.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final AccountVerificationProperties policy;

    public RedisAccountVerificationStore(StringRedisTemplate redis, AccountVerificationProperties policy) {
        this.redis = redis;
        this.policy = policy;
    }

    @Override
    public Duration reserve(Subject subject, String remoteAddress, String requestId, String codeDigest) {
        String member = Long.toString(subject.userId());
        List<?> result = execute(RESERVE, List.of(stateKey(subject), cooldownKey(subject),
                        PREFIX + "account-limit:" + member,
                        PREFIX + "ip-limit:" + VerificationDigests.sha256(remoteAddress), proofKey(subject)),
                requestId, codeDigest, millis(policy.codeTtlSeconds()), millis(policy.resendCooldownSeconds()),
                millis(policy.rateWindowSeconds()), Integer.toString(policy.maxRequestsPerAccount()),
                Integer.toString(policy.maxRequestsPerIp()), member, emailHash(subject), subject.purpose().name(),
                Long.toString(subject.authVersion()));
        long outcome = number(result, 0);
        if (outcome == 0) return Duration.ZERO;
        if (outcome == 1 && number(result, 1) > 0) return Duration.ofMillis(number(result, 1));
        throw new IllegalStateException("Unexpected account verification reservation result");
    }

    @Override
    public Optional<ActiveRequest> activate(Subject subject, String requestId) {
        List<?> result = execute(ACTIVATE, List.of(stateKey(subject), cooldownKey(subject)), requestId);
        long ttl = number(result, 0);
        if (ttl <= 0) return Optional.empty();
        return Optional.of(new ActiveRequest(seconds(ttl), seconds(number(result, 1))));
    }

    @Override
    public void discard(Subject subject, String requestId) {
        execute(DISCARD, List.of(stateKey(subject)), requestId);
    }

    @Override
    public ConfirmationResult confirm(Subject subject, String requestId, String codeDigest, String proofDigest) {
        List<?> result = execute(CONFIRM, List.of(stateKey(subject), proofKey(subject)),
                requestId, codeDigest, proofDigest, Integer.toString(policy.maxVerificationAttempts()),
                millis(policy.proofTtlSeconds()), Long.toString(subject.userId()), emailHash(subject),
                subject.purpose().name(), Long.toString(subject.authVersion()));
        ConfirmationStatus status = switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ConfirmationStatus.CONFIRMED;
            case 1 -> ConfirmationStatus.EXPIRED;
            case 2 -> ConfirmationStatus.SUPERSEDED;
            case 3 -> ConfirmationStatus.INVALID_CODE;
            case 4 -> ConfirmationStatus.ATTEMPTS_EXCEEDED;
            case 5 -> ConfirmationStatus.ALREADY_CONFIRMED;
            default -> throw new IllegalStateException("Unexpected account verification confirmation result");
        };
        long ttl = number(result, 1);
        if (status == ConfirmationStatus.CONFIRMED && ttl <= 0) {
            throw new IllegalStateException("Invalid account proof lifetime");
        }
        return new ConfirmationResult(status, status == ConfirmationStatus.CONFIRMED ? seconds(ttl) : 0);
    }

    @Override
    public ProofConsumption consumeProof(Subject subject, String proofDigest) {
        List<?> result = execute(CONSUME, List.of(proofKey(subject)), proofDigest,
                Long.toString(subject.userId()), emailHash(subject), subject.purpose().name(),
                Long.toString(subject.authVersion()));
        ProofStatus status = switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ProofStatus.CONSUMED;
            case 1 -> ProofStatus.EXPIRED;
            case 2 -> ProofStatus.INVALID;
            case 3 -> ProofStatus.ALREADY_USED;
            default -> throw new IllegalStateException("Unexpected account proof consumption result");
        };
        long verifiedAt = number(result, 1);
        if (status == ProofStatus.CONSUMED && verifiedAt <= 0) {
            throw new IllegalStateException("Invalid account verification timestamp");
        }
        return new ProofConsumption(status, status == ProofStatus.CONSUMED ? Instant.ofEpochMilli(verifiedAt) : null);
    }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            T result = redis.execute(script, keys, (Object[]) args);
            if (result == null) throw new IllegalStateException("Missing account verification script result");
            return result;
        } catch (DataAccessException exception) {
            log.warn("Account verification Redis operation failed: {}", exception.getClass().getSimpleName());
            throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE, exception);
        }
    }

    private static long number(List<?> result, int index) {
        if (result.size() != 2 || !(result.get(index) instanceof Number number)) {
            throw new IllegalStateException("Unexpected account verification script result");
        }
        return number.longValue();
    }

    private static String emailHash(Subject subject) {
        return VerificationDigests.sha256(subject.email());
    }

    private static String suffix(Subject subject) {
        return subject.userId() + ":" + emailHash(subject) + ":" + subject.purpose().name()
                + ":" + subject.authVersion();
    }

    private static String stateKey(Subject subject) {
        return PREFIX + "state:" + suffix(subject);
    }

    private static String proofKey(Subject subject) {
        return PREFIX + "proof:" + suffix(subject);
    }

    private static String cooldownKey(Subject subject) {
        return PREFIX + "cooldown:" + subject.userId();
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
