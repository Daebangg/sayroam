package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.daebangg.sayroam.global.web.RequestLoggingFilter;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(LoggingHttpIntegrationTest.ProbeConfig.class)
class LoggingHttpIntegrationTest extends DatabaseIntegrationTest {
    @LocalServerPort int port;
    @Autowired JsonMapper json;

    @Test
    void realServletAsyncDispatchPreservesTheRequestIdAndLogsOnlyTheFinalStatus() throws Exception {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class);
             HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request("/api/logging-probe/async?code=private-code"),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            String requestId = response.headers().firstValue("X-Request-ID").orElseThrow();
            JsonNode body = json.readTree(response.body());
            assertThat(body.path("requestId").asString()).isEqualTo(requestId);
            assertThat(body.path("taskId").asString()).isNotBlank();
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(capture.events()).hasSize(1));
            ILoggingEvent event = capture.events().getFirst();
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", requestId);
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("status");
                assertThat(pair.value).isEqualTo(200);
            });
            assertThat(event.getKeyValuePairs().toString()).contains("/api/logging-probe/async")
                    .doesNotContain("private-code");
            HttpResponse<String> denied = client.send(request("/api/users/me"), HttpResponse.BodyHandlers.ofString());
            assertThat(denied.statusCode()).isEqualTo(401);
            assertThat(denied.headers().firstValue("X-Request-ID").orElseThrow()).isNotEqualTo(requestId);
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(capture.events()).hasSize(2));
        }
    }

    @Test
    void realUnexpectedFailureKeepsTheErrorResponseWithoutLoggingItsPrivateMessage() throws Exception {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class);
             HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request("/api/logging-probe/failure"), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(500);
            assertThat(json.readTree(response.body()).path("code").asString()).isEqualTo("INTERNAL_SERVER_ERROR");
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(capture.events()).hasSize(1));
            assertThat(capture.events().getFirst().getKeyValuePairs().toString())
                    .contains("IllegalStateException", "ProbeController.failure").doesNotContain("private-secret");
        }
    }

    @Test
    void realAsyncTimeoutRecoveryLogsTheSuccessfulFinalStatus() throws Exception {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class);
             HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = client.send(request("/api/logging-probe/timeout-recovered"), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.body()).isEqualTo("recovered");
            await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(capture.events()).hasSize(1));
            ILoggingEvent event = capture.events().getFirst();
            assertThat(event.getLevel()).isEqualTo(ch.qos.logback.classic.Level.INFO);
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", response.headers().firstValue("X-Request-ID").orElseThrow());
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("status");
                assertThat(pair.value).isEqualTo(200);
            }).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("timedOut");
                assertThat(pair.value).isEqualTo(true);
            });
        }
    }

    private HttpRequest request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10)).GET().build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfig {
        @Bean
        ProbeController loggingProbeController() {
            return new ProbeController();
        }

        @Bean
        @Order(0)
        SecurityFilterChain loggingProbeSecurity(HttpSecurity http) throws Exception {
            return http.securityMatcher("/api/logging-probe/**")
                    .authorizeHttpRequests(rules -> rules.anyRequest().permitAll()).build();
        }
    }

    @RestController
    static class ProbeController {
        @GetMapping("/api/logging-probe/async")
        Callable<Map<String, String>> async() {
            return () -> Map.of("requestId", MDC.get("requestId"), "taskId", MDC.get("taskId"));
        }

        @GetMapping("/api/logging-probe/failure")
        String failure() {
            throw new IllegalStateException("private-secret");
        }

        @GetMapping("/api/logging-probe/timeout-recovered")
        DeferredResult<String> timeoutRecovered() {
            DeferredResult<String> result = new DeferredResult<>(50L);
            result.onTimeout(() -> result.setResult("recovered"));
            return result;
        }
    }
}
