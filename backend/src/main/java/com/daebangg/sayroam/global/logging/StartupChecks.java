package com.daebangg.sayroam.global.logging;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.mail.autoconfigure.MailSenderValidatorAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 기동 시 실제 의존성 검사를 병렬 실행한다. 실패·시간 초과도 표시하며 앱을 강제 종료하지 않는다. */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.logging.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupChecks implements AutoCloseable {
    private final ConfigurableApplicationContext context;
    private final long timeoutMs;
    private final OAuthEndpointProbe endpointProbe;
    private final OAuthProbe oauthProbe;
    private final ExecutorService executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("startup-check-", 0).factory());

    @Autowired
    public StartupChecks(ConfigurableApplicationContext context, @Value("${app.logging.startup.timeout-ms:3000}") long timeoutMs) {
        this(context, timeoutMs, null);
    }

    StartupChecks(ConfigurableApplicationContext context, long timeoutMs, OAuthProbe oauthProbe) {
        if (timeoutMs < 1 || timeoutMs > 30000) throw new IllegalArgumentException("Startup timeout must be 1..30000 ms");
        this.context = context;
        this.timeoutMs = timeoutMs;
        this.endpointProbe = new OAuthEndpointProbe(timeoutMs);
        this.oauthProbe = oauthProbe == null ? endpointProbe::check : oauthProbe;
    }

    public Report checkAll() {
        long startedAt = System.nanoTime();
        long deadline = startedAt + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        List<Pending> pending = List.of(
                submit("Database", "MySQL", this::database),
                submit("Cache", "Redis", this::redis),
                submit("Email", "SMTP", this::mail),
                submit("OAuth", "Google", () -> oauth("google")),
                submit("OAuth", "Kakao", () -> oauth("kakao")),
                submit("OAuth", "Naver", () -> oauth("naver")));
        List<Result> results = new ArrayList<>();
        for (Pending check : pending) {
            try {
                Outcome outcome = check.future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
                results.add(new Result(check.service, check.stack, outcome.status, outcome.proof, outcome.durationMs));
            } catch (TimeoutException exception) {
                check.future.cancel(true);
                log.atWarn().addKeyValue("event", "startup_check_timeout").addKeyValue("service", check.service)
                        .addKeyValue("stack", check.stack).log("Startup dependency check timed out");
                results.add(new Result(check.service, check.stack, Status.TIMEOUT, "timeout", timeoutMs));
            } catch (ExecutionException exception) {
                SafeLog.atWarn(log, exception.getCause()).addKeyValue("event", "startup_check_failed")
                        .addKeyValue("service", check.service).addKeyValue("stack", check.stack)
                        .log("Startup dependency check failed");
                results.add(new Result(check.service, check.stack, Status.FAILED, "probe failed", elapsed(startedAt)));
            } catch (InterruptedException exception) {
                pending.forEach(task -> task.future.cancel(true));
                Thread.currentThread().interrupt();
                results.add(new Result(check.service, check.stack, Status.TIMEOUT, "interrupted", elapsed(startedAt)));
            }
        }
        return new Report(List.copyOf(results), elapsed(startedAt));
    }

    private Pending submit(String service, String stack, Callable<Outcome> action) {
        Future<Outcome> future = executor.submit(() -> {
            long startedAt = System.nanoTime();
            try (LogContext ignored = LogContext.task(null)) {
                Outcome outcome = action.call();
                return new Outcome(outcome.status, outcome.proof, elapsed(startedAt));
            }
        });
        return new Pending(service, stack, future);
    }

    private Outcome database() throws Exception {
        DataSource source = context.getBeanProvider(DataSource.class).getIfAvailable();
        if (source == null) return new Outcome(Status.MISSING, "no datasource", 0);
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(Math.max(1, (int) Math.ceil(timeoutMs / 1000.0)));
            try (ResultSet result = statement.executeQuery("SELECT 1")) {
                if (!result.next() || result.getInt(1) != 1) throw new IllegalStateException("Database probe result invalid");
            }
        }
        return new Outcome(Status.VERIFIED, "SELECT 1", 0);
    }

    private Outcome redis() {
        StringRedisTemplate redis = context.getBeanProvider(StringRedisTemplate.class).getIfAvailable();
        if (redis == null) return new Outcome(Status.MISSING, "no Redis bean", 0);
        String response = redis.execute((RedisCallback<String>) connection -> connection.ping());
        if (!"PONG".equals(response)) throw new IllegalStateException("Redis PING did not return PONG");
        return new Outcome(Status.VERIFIED, "PING / PONG", 0);
    }

    private Outcome mail() throws Exception {
        if (!enabled("app.mail.enabled")) return new Outcome(Status.DISABLED, "", 0);
        for (String name : context.getBeanFactory().getBeanNamesForType(MailSenderValidatorAutoConfiguration.class, false, false)) {
            if (context.getBeanFactory().containsSingleton(name)) return new Outcome(Status.VERIFIED, "Boot SMTP check", 0);
        }
        JavaMailSenderImpl sender = context.getBeanProvider(JavaMailSenderImpl.class).getIfAvailable();
        if (sender == null) return new Outcome(Status.MISSING, "no SMTP sender", 0);
        sender.testConnection();
        return new Outcome(Status.VERIFIED, "SMTP connect", 0);
    }

    private Outcome oauth(String provider) throws Exception {
        if (!enabled("app.auth.social." + provider + ".enabled")) return new Outcome(Status.DISABLED, "", 0);
        ClientRegistrationRepository clients = context.getBeanProvider(ClientRegistrationRepository.class).getIfAvailable();
        ClientRegistration registration = clients == null ? null : clients.findByRegistrationId(provider);
        if (registration == null) return new Outcome(Status.MISSING, "no client", 0);
        return new Outcome(Status.REACHABLE, oauthProbe.check(provider, registration), 0);
    }

    private boolean enabled(String property) {
        return context.getEnvironment().getProperty(property, Boolean.class, false);
    }

    private long elapsed(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    @PreDestroy
    @Override
    public void close() {
        executor.shutdownNow();
        endpointProbe.close();
    }

    @FunctionalInterface
    interface OAuthProbe {
        String check(String provider, ClientRegistration registration) throws Exception;
    }

    public enum Status { VERIFIED, REACHABLE, DISABLED, MISSING, FAILED, TIMEOUT }
    public record Result(String service, String stack, Status status, String proof, long durationMs) { }
    public record Report(List<Result> results, long durationMs) { }
    private record Outcome(Status status, String proof, long durationMs) { }
    private record Pending(String service, String stack, Future<Outcome> future) { }
}
