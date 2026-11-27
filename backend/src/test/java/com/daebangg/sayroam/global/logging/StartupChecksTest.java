package com.daebangg.sayroam.global.logging;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupChecksTest {
    @Test
    void verifiesSqlPingAndSmtpButLabelsOAuthAsReachabilityOnly() throws Exception {
        DataSource source = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet rows = mock(ResultSet.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery("SELECT 1")).thenReturn(rows);
        when(rows.next()).thenReturn(true);
        when(rows.getInt(1)).thenReturn(1);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        RedisConnection redisConnection = mock(RedisConnection.class);
        when(redisConnection.ping()).thenReturn("PONG");
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisCallback<String>>any()))
                .thenAnswer(invocation -> invocation.<RedisCallback<String>>getArgument(0).doInRedis(redisConnection));
        JavaMailSenderImpl mail = mock(JavaMailSenderImpl.class);
        AtomicInteger oauthRequests = new AtomicInteger();
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment().withProperty("app.mail.enabled", "true")
                    .withProperty("app.auth.social.google.enabled", "true"));
            context.registerBean(DataSource.class, () -> source);
            context.registerBean(StringRedisTemplate.class, () -> redis);
            context.registerBean(JavaMailSenderImpl.class, () -> mail);
            context.registerBean(ClientRegistrationRepository.class, () -> provider -> google());
            context.refresh();
            try (StartupChecks checks = new StartupChecks(context, 2000, (provider, registration) -> {
                oauthRequests.incrementAndGet();
                return "OIDC HTTP 200";
            })) {
                StartupChecks.Report report = checks.checkAll();
                assertThat(result(report, "MySQL").status()).isEqualTo(StartupChecks.Status.VERIFIED);
                assertThat(result(report, "Redis").proof()).isEqualTo("PING / PONG");
                assertThat(result(report, "SMTP").status()).isEqualTo(StartupChecks.Status.VERIFIED);
                assertThat(result(report, "Google").status()).isEqualTo(StartupChecks.Status.REACHABLE);
                assertThat(result(report, "Kakao").status()).isEqualTo(StartupChecks.Status.DISABLED);
            }
            verify(statement).executeQuery("SELECT 1");
            verify(connection).close();
            verify(redisConnection).ping();
            verify(mail).testConnection();
            assertThat(oauthRequests).hasValue(1);
        }
    }

    @Test
    void doesNotMarkAnInvalidPingAsVerifiedOrLeakItsExceptionMessage() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(org.mockito.ArgumentMatchers.<RedisCallback<String>>any()))
                .thenThrow(new IllegalStateException("private-redis-password"));
        try (GenericApplicationContext context = new GenericApplicationContext();
             LogCapture capture = new LogCapture(StartupChecks.class)) {
            context.registerBean(StringRedisTemplate.class, () -> redis);
            context.refresh();
            try (StartupChecks checks = new StartupChecks(context, 2000, (provider, client) -> "unused")) {
                assertThat(result(checks.checkAll(), "Redis").status()).isEqualTo(StartupChecks.Status.FAILED);
                assertThat(capture.events()).hasSize(1);
                assertThat(capture.events().getFirst().getKeyValuePairs().toString()).doesNotContain("private-redis-password");
            }
        }
    }

    @Test
    void boundsTheEntireParallelCheckWindowAndShowsTimeoutInsteadOfSuccess() {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment().withProperty("app.auth.social.google.enabled", "true"));
            context.registerBean(ClientRegistrationRepository.class, () -> provider -> google());
            context.refresh();
            try (StartupChecks checks = new StartupChecks(context, 100, (provider, registration) -> {
                Thread.sleep(5000);
                return "not reached";
            })) {
                StartupChecks.Report report = checks.checkAll();
                assertThat(result(report, "Google").status()).isEqualTo(StartupChecks.Status.TIMEOUT);
                assertThat(report.durationMs()).isLessThan(1500);
            }
        }
    }

    static ClientRegistration google() {
        return ClientRegistration.withRegistrationId("google").clientId("private-client").clientSecret("private-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE).redirectUri("http://localhost/callback")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth").tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo").userNameAttributeName("id").build();
    }

    private StartupChecks.Result result(StartupChecks.Report report, String stack) {
        return report.results().stream().filter(result -> result.stack().equals(stack)).findFirst().orElseThrow();
    }
}
