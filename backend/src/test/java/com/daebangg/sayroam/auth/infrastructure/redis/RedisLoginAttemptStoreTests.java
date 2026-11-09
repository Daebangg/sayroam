package com.daebangg.sayroam.auth.infrastructure.redis;

import com.daebangg.sayroam.auth.application.port.out.LoginAttemptStore.Outcome;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.infrastructure.config.LoginAttemptProperties;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RedisLoginAttemptStoreTests extends DatabaseIntegrationTest {
    @Autowired StringRedisTemplate redis;
    @Autowired RedisLoginAttemptStore store;
    private static final String EMAIL = "limiter@example.test";
    private static final String IP = "192.0.2.85";

    @BeforeEach
    void clean() {
        var keys = redis.keys("sayroam:auth:{login-attempts}:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
    }

    @Test
    void fifthFailureBlocksFor300SecondsAndBlockedRequestsDoNotExtendIt() {
        for (int i = 0; i < 4; i++) {
            var result = store.evaluate(EMAIL, IP, Outcome.FAILURE);
            assertThat(result.retryAfter()).isZero();
            assertThat(result.remainingAttempts()).isEqualTo(4 - i);
        }
        assertThat(store.evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter()).isEqualTo(Duration.ofSeconds(300));
        var key = key("email", EMAIL);
        var deadline = redis.opsForHash().get(key, "blockedUntil");
        for (var outcome : Outcome.values()) assertThat(store.evaluate(EMAIL, IP, outcome).retryAfter()).isPositive().isLessThanOrEqualTo(Duration.ofSeconds(300));
        assertThat(redis.opsForHash().get(key, "blockedUntil")).isEqualTo(deadline);
        assertThat(redis.opsForHash().get(key, "failures")).isEqualTo("5");
        assertThat(redis.getExpire(key, TimeUnit.SECONDS)).isBetween(298L, 300L);
    }

    @Test
    void remainingAttemptsBelongsToEmailEvenWhenIpIsNearItsSeparateLimit() {
        for (int i = 0; i < 98; i++) {
            assertThat(store.evaluate("user" + i + "@example.test", IP, Outcome.FAILURE).retryAfter()).isZero();
        }
        var firstFailure = store.evaluate(EMAIL, IP, Outcome.FAILURE);
        assertThat(firstFailure.retryAfter()).isZero();
        assertThat(firstFailure.remainingAttempts()).isEqualTo(4);
        var blocked = store.evaluate("new@example.test", IP, Outcome.FAILURE);
        assertThat(blocked.remainingAttempts()).isZero();
        assertThat(blocked.retryAfter()).isEqualTo(Duration.ofSeconds(300));
        var deadline = redis.opsForHash().get(key("ip", IP), "blockedUntil");
        for (var outcome : Outcome.values()) {
            assertThat(store.evaluate(EMAIL, IP, outcome).retryAfter()).isPositive();
        }
        assertThat(redis.opsForHash().get(key("ip", IP), "blockedUntil")).isEqualTo(deadline);
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("100");
    }

    @Test
    void emailIsSharedAcrossIpsAndIpBlocksOnlyAfter100FailuresAcrossEmails() {
        for (int i = 0; i < 5; i++) store.evaluate(EMAIL, "192.0.2." + i, Outcome.FAILURE);
        assertThat(store.evaluate(EMAIL, "198.51.100.1", Outcome.CHECK).retryAfter()).isPositive();
        assertThat(store.evaluate("other@example.test", "198.51.100.1", Outcome.CHECK).retryAfter()).isZero();
        for (int i = 0; i < 99; i++) {
            assertThat(store.evaluate("user" + i + "@example.test", IP, Outcome.FAILURE).retryAfter()).isZero();
        }
        assertThat(store.evaluate("new@example.test", IP, Outcome.CHECK).retryAfter()).isZero();
        assertThat(store.evaluate("new@example.test", IP, Outcome.FAILURE).retryAfter()).isEqualTo(Duration.ofSeconds(300));
        assertThat(store.evaluate("another@example.test", IP, Outcome.CHECK).retryAfter()).isPositive();
        assertThat(store.evaluate("another@example.test", "198.51.100.1", Outcome.CHECK).retryAfter()).isZero();
    }

    @Test
    void successfulPasswordResetsEmailSoTheNextFailureHasFourAttemptsWhileIpHistoryRemains() {
        for (int i = 0; i < 4; i++) store.evaluate(EMAIL, IP, Outcome.FAILURE);
        assertThat(store.evaluate(EMAIL, IP, Outcome.SUCCESS).remainingAttempts()).isEqualTo(5);
        assertThat(redis.hasKey(key("email", EMAIL))).isFalse();
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("4");
        var nextFailure = store.evaluate(EMAIL, IP, Outcome.FAILURE);
        assertThat(nextFailure.retryAfter()).isZero();
        assertThat(nextFailure.remainingAttempts()).isEqualTo(4);
        assertThat(redis.opsForHash().get(key("email", EMAIL), "failures")).isEqualTo("1");
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("5");
    }

    @Test
    void previousFiveAttemptIpBucketDoesNotLockTheNewWiderPolicy() {
        String oldIpKey = "sayroam:auth:{login-attempts}:ip:" + VerificationDigests.sha256(IP);
        redis.opsForHash().putAll(oldIpKey, java.util.Map.of("failures", "5", "blockedUntil", Long.toString(System.currentTimeMillis() + 300000)));
        redis.expire(oldIpKey, Duration.ofSeconds(300));
        var oldState = redis.opsForHash().entries(oldIpKey);
        assertThat(store.evaluate(EMAIL, IP, Outcome.CHECK).retryAfter()).isZero();
        var result = store.evaluate(EMAIL, IP, Outcome.FAILURE);
        assertThat(result.retryAfter()).isZero();
        assertThat(result.remainingAttempts()).isEqualTo(4);
        assertThat(redis.opsForHash().entries(oldIpKey)).isEqualTo(oldState);
    }

    @Test
    void observationAndBlockExpirationAllowAFreshAttempt() {
        store.evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter();
        expireBoth();
        store.evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter();
        assertThat(redis.opsForHash().get(key("email", EMAIL), "failures")).isEqualTo("1");
        for (int i = 0; i < 4; i++) store.evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter();
        expireBoth();
        assertThat(store.evaluate(EMAIL, IP, Outcome.CHECK).retryAfter()).isZero();
        assertThat(store.evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter()).isZero();
    }

    @Test
    void concurrentResultsAcrossTwoInstancesCannotLoseCountsOrExtendTheBlock() throws Exception {
        var otherInstance = new RedisLoginAttemptStore(redis, new LoginAttemptProperties(5, 100, 300, 300));
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var work = IntStream.range(0, 32).mapToObj(i -> pool.submit(() ->
                    (i % 2 == 0 ? store : otherInstance).evaluate(EMAIL, IP, Outcome.FAILURE).retryAfter())).toList();
            int allowed = 0;
            for (var result : work) if (result.get(10, TimeUnit.SECONDS).isZero()) allowed++;
            assertThat(allowed).isEqualTo(4);
        }
        assertThat(redis.opsForHash().get(key("email", EMAIL), "failures")).isEqualTo("5");
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("5");
    }

    @Test
    void ipObservationAndBlockExpireWithoutExtendingOnRequests() {
        store.evaluate(EMAIL, IP, Outcome.FAILURE);
        expireBoth();
        store.evaluate(EMAIL, IP, Outcome.FAILURE);
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("1");
        for (int i = 0; i < 99; i++) store.evaluate("user" + i + "@example.test", IP, Outcome.FAILURE);
        assertThat(store.evaluate(EMAIL, IP, Outcome.CHECK).retryAfter()).isPositive();
        expireBoth();
        assertThat(store.evaluate(EMAIL, IP, Outcome.FAILURE).remainingAttempts()).isEqualTo(4);
    }

    @Test
    void concurrentFailuresAcrossEmailsReachTheIpLimitExactlyOnceAcrossInstances() throws Exception {
        var otherInstance = new RedisLoginAttemptStore(redis, new LoginAttemptProperties(5, 100, 300, 300));
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            var work = IntStream.range(0, 128).mapToObj(i -> pool.submit(() ->
                    (i % 2 == 0 ? store : otherInstance).evaluate("user" + i + "@example.test", IP, Outcome.FAILURE))).toList();
            int allowed = 0;
            for (var future : work) {
                var result = future.get(10, TimeUnit.SECONDS);
                if (result.retryAfter().isZero()) {
                    assertThat(result.remainingAttempts()).isEqualTo(4);
                    allowed++;
                } else assertThat(result.remainingAttempts()).isZero();
            }
            assertThat(allowed).isEqualTo(99);
        }
        assertThat(redis.opsForHash().get(key("ip", IP), "failures")).isEqualTo("100");
    }

    private void expireBoth() {
        var keys = List.of(key("email", EMAIL), key("ip", IP));
        keys.forEach(key -> redis.expire(key, Duration.ofMillis(1)));
        await().atMost(Duration.ofSeconds(2)).until(() -> keys.stream().noneMatch(key -> Boolean.TRUE.equals(redis.hasKey(key))));
    }

    private String key(String kind, String value) {
        return "sayroam:auth:{login-attempts}:" + (kind.equals("ip") ? "ip-wide" : kind) + ":" + VerificationDigests.sha256(value);
    }
}
