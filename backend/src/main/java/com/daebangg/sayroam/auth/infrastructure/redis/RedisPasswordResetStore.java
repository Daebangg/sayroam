package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.PasswordResetStore;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.infrastructure.config.PasswordResetProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
public class RedisPasswordResetStore implements PasswordResetStore {
    private static final String PREFIX = "sayroam:auth:{password-reset}:";
    private static final String PURPOSE = "PASSWORD_RESET";
    // 상태 형식·원자성은 기존 검증 스크립트를 공유하고 namespace·정책·바인딩은 분리한다.
    private static final DefaultRedisScript<List> RESERVE = script("account-verification-reserve.lua");
    private static final DefaultRedisScript<List> ACTIVATE = script("email-verification-activate.lua");
    private static final DefaultRedisScript<List> CONFIRM = script("account-verification-confirm.lua");
    private static final DefaultRedisScript<List> CONSUME = script("account-proof-consume.lua");
    private static final DefaultRedisScript<Long> DISCARD = new DefaultRedisScript<>();
    static {
        DISCARD.setLocation(new ClassPathResource("redis/email-verification-discard.lua"));
        DISCARD.setResultType(Long.class);
    }

    private final StringRedisTemplate redis;
    private final PasswordResetProperties policy;

    public RedisPasswordResetStore(StringRedisTemplate redis, PasswordResetProperties policy) {
        this.redis = redis;
        this.policy = policy;
    }

    @Override
    public Duration reserve(Subject subject, String remoteAddress, String requestId, String codeDigest) {
        String emailHash = VerificationDigests.sha256(subject.email());
        List<?> result = execute(RESERVE, List.of(key("state", subject), key("cooldown", subject),
                        key("email-limit", subject), PREFIX + "ip-limit:" + VerificationDigests.sha256(remoteAddress),
                        key("proof", subject)),
                requestId, codeDigest, millis(policy.codeTtlSeconds()), millis(policy.resendCooldownSeconds()),
                millis(policy.rateWindowSeconds()), Integer.toString(policy.maxRequestsPerEmail()),
                Integer.toString(policy.maxRequestsPerIp()), Long.toString(subject.userId()), emailHash, PURPOSE,
                Long.toString(subject.authVersion()));
        long outcome = number(result, 0);
        if (outcome == 0) return Duration.ZERO;
        if (outcome == 1 && number(result, 1) > 0) return Duration.ofMillis(number(result, 1));
        throw new IllegalStateException("Unexpected password reset reservation result");
    }

    @Override
    public Optional<ActiveRequest> activate(Subject subject, String requestId) {
        List<?> result = execute(ACTIVATE, List.of(key("state", subject), key("cooldown", subject)), requestId);
        long ttl = number(result, 0);
        return ttl <= 0 ? Optional.empty() : Optional.of(new ActiveRequest(seconds(ttl), seconds(number(result, 1))));
    }

    @Override
    public void discard(Subject subject, String requestId) {
        execute(DISCARD, List.of(key("state", subject)), requestId);
    }

    @Override
    public ConfirmationResult confirm(Subject subject, String requestId, String codeDigest, String proofDigest) {
        List<?> result = execute(CONFIRM, List.of(key("state", subject), key("proof", subject)),
                requestId, codeDigest, proofDigest, Integer.toString(policy.maxVerificationAttempts()),
                millis(policy.proofTtlSeconds()), Long.toString(subject.userId()),
                VerificationDigests.sha256(subject.email()), PURPOSE, Long.toString(subject.authVersion()));
        PasswordResetStore.ConfirmationStatus status = switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ConfirmationStatus.CONFIRMED;
            case 1 -> ConfirmationStatus.EXPIRED;
            case 2 -> ConfirmationStatus.SUPERSEDED;
            case 3 -> ConfirmationStatus.INVALID_CODE;
            case 4 -> ConfirmationStatus.ATTEMPTS_EXCEEDED;
            case 5 -> ConfirmationStatus.ALREADY_CONFIRMED;
            default -> throw new IllegalStateException("Unexpected password reset confirmation result");
        };
        long ttl = number(result, 1);
        if (status == ConfirmationStatus.CONFIRMED && ttl <= 0) throw new IllegalStateException("Invalid reset proof lifetime");
        return new ConfirmationResult(status, status == ConfirmationStatus.CONFIRMED ? seconds(ttl) : 0);
    }

    @Override
    public ProofStatus consumeProof(Subject subject, String proofDigest) {
        List<?> result = execute(CONSUME, List.of(key("proof", subject)), proofDigest,
                Long.toString(subject.userId()), VerificationDigests.sha256(subject.email()), PURPOSE,
                Long.toString(subject.authVersion()));
        return switch (Math.toIntExact(number(result, 0))) {
            case 0 -> ProofStatus.CONSUMED;
            case 1 -> ProofStatus.EXPIRED;
            case 2 -> ProofStatus.INVALID;
            case 3 -> ProofStatus.ALREADY_USED;
            default -> throw new IllegalStateException("Unexpected password reset proof result");
        };
    }

    private <T> T execute(DefaultRedisScript<T> script, List<String> keys, String... args) {
        try {
            T result = redis.execute(script, keys, (Object[]) args);
            if (result == null) throw new IllegalStateException("Missing password reset script result");
            return result;
        } catch (DataAccessException exception) {
            log.warn("Password reset Redis operation failed: {}", exception.getClass().getSimpleName());
            throw new AuthException(AuthErrorCode.EMAIL_VERIFICATION_UNAVAILABLE, exception);
        }
    }

    private static String key(String type, Subject subject) {
        // 계정 생성·탈퇴·비밀번호 변경으로 발송 제한을 초기화할 수 없게 이메일 기준으로 고정한다.
        return PREFIX + type + ":" + VerificationDigests.sha256(subject.email());
    }
    private static long number(List<?> result, int index) {
        if (result.size() != 2 || !(result.get(index) instanceof Number number)) throw new IllegalStateException("Unexpected reset script result");
        return number.longValue();
    }
    private static String millis(int seconds) { return Long.toString(seconds * 1000L); }
    private static long seconds(long millis) { return Math.max(0, (millis + 999) / 1000); }
    private static DefaultRedisScript<List> script(String fileName) {
        DefaultRedisScript<List> script = new DefaultRedisScript<List>();
        script.setLocation(new ClassPathResource("redis/" + fileName));
        script.setResultType(List.class);
        return script;
    }
}
