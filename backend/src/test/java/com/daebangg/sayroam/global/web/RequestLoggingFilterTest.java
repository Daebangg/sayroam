package com.daebangg.sayroam.global.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.exception.GlobalExceptionHandler;
import com.daebangg.sayroam.global.logging.LogCapture;
import com.daebangg.sayroam.global.security.SecurityProblemHandler;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class RequestLoggingFilterTest {
    private final RequestLoggingFilter filter = new RequestLoggingFilter(1000);
    private LogCapture capture;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        capture = new LogCapture(RequestLoggingFilter.class);
        mvc = standaloneSetup(new Endpoints())
                .setControllerAdvice(new GlobalExceptionHandler(new RefreshTokenCookie(new RefreshCookieProperties(true))))
                .addFilters(filter).build();
    }

    @AfterEach
    void close() {
        capture.close();
        MDC.clear();
    }

    @Test
    void connectsResponseAndApplicationLogsWithoutReadingSecretsOrRawPaths() throws Exception {
        String id = mvc.perform(get("/api/logging/private-path")
                        .queryParam("code", "secret-code").header("Authorization", "Bearer secret-token")
                        .header("Cookie", "session=secret-cookie").header("X-Request-ID", "untrusted-id"))
                .andExpect(status().isOk()).andReturn().getResponse().getHeader("X-Request-ID");
        assertThat(UUID.fromString(id)).isNotNull();
        ILoggingEvent event = capture.events().getFirst();
        assertThat(event.getMDCPropertyMap()).containsEntry("requestId", id);
        assertThat(fields(event)).containsEntry("route", "/api/logging/{value}").containsEntry("status", 200);
        assertThat(fields(event).toString()).doesNotContain("private-path", "secret-code", "secret-token", "secret-cookie", "untrusted-id");
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        String next = mvc.perform(get("/api/logging/another"))
                .andReturn().getResponse().getHeader("X-Request-ID");
        assertThat(next).isNotEqualTo(id);
    }

    @Test
    void unexpectedFailuresAreRecordedOnceWithSafeDiagnosticsAndTheSameResponseContract() throws Exception {
        mvc.perform(get("/api/logging-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
        assertThat(capture.events()).hasSize(1);
        ILoggingEvent event = capture.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(fields(event)).containsEntry("status", 500).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
        assertThat(fields(event).toString()).contains("IllegalStateException", "Endpoints.fail").doesNotContain("secret-body");
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    void businessErrorsHaveACodeAndStatusWithoutAnErrorStack() throws Exception {
        mvc.perform(get("/api/logging-business"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("EMAIL_CODE_INVALID"));
        assertThat(capture.events()).hasSize(1);
        ILoggingEvent event = capture.events().getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.INFO);
        assertThat(fields(event)).containsEntry("errorCode", "EMAIL_CODE_INVALID").doesNotContainKey("exceptionFrames");
    }

    @Test
    void securityRejectionsAreLoggedWithoutEchoingUnmappedSensitivePaths() throws Exception {
        SecurityProblemHandler handler = new SecurityProblemHandler(JsonMapper.builder().build());
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/private-address@example.test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> handler.commence((HttpServletRequest) incoming,
                (HttpServletResponse) outgoing, new InsufficientAuthenticationException("secret-token")));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
        assertThat(response.getHeader("X-Request-ID")).isNotBlank();
        assertThat(fields(capture.events().getFirst())).containsEntry("route", "UNMAPPED").containsEntry("status", 401)
                .containsEntry("errorCode", "UNAUTHENTICATED");
        capture.events().clear();
        MockHttpServletResponse denied = new MockHttpServletResponse();
        filter.doFilter(new MockHttpServletRequest("GET", "/api/admin/private"), denied,
                (incoming, outgoing) -> handler.handle((HttpServletRequest) incoming, (HttpServletResponse) outgoing,
                        new AccessDeniedException("secret-token")));
        assertThat(denied.getStatus()).isEqualTo(403);
        assertThat(fields(capture.events().getFirst())).containsEntry("status", 403).containsEntry("errorCode", "ACCESS_DENIED");
    }

    @Test
    void asyncCompletionUsesTheFinalStatusAndDoesNotLogAgainOnRedispatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/async");
        request.setAsyncSupported(true);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, (incoming, outgoing) -> incoming.startAsync());
        assertThat(capture.events()).isEmpty();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
        String id = response.getHeader("X-Request-ID");
        request.setDispatcherType(DispatcherType.ASYNC);
        filter.doFilter(request, response, (incoming, outgoing) -> {
            assertThat(MDC.get("requestId")).isEqualTo(id);
            incoming.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/async");
            ((HttpServletResponse) outgoing).setStatus(202);
        });
        AsyncContext context = request.getAsyncContext();
        context.complete();
        assertThat(capture.events()).hasSize(1);
        assertThat(fields(capture.events().getFirst())).containsEntry("status", 202).containsEntry("route", "/api/async");
        assertThat(capture.events().getFirst().getMDCPropertyMap()).containsEntry("requestId", id);
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    @Test
    void successfulPollingAndStaticAssetsAreDebugButSlowOrFailedRequestsRemainVisible() throws Exception {
        mvc.perform(get("/api/notifications/summary")).andExpect(status().isOk());
        assertThat(capture.events().getFirst().getLevel()).isEqualTo(Level.DEBUG);
        capture.events().clear();
        filter.doFilter(new MockHttpServletRequest("GET", "/assets/private-file.js"), new MockHttpServletResponse(), (a, b) -> {});
        assertThat(capture.events().getFirst().getLevel()).isEqualTo(Level.DEBUG);
        assertThat(fields(capture.events().getFirst())).containsEntry("route", "UNMAPPED");
        capture.events().clear();
        new RequestLoggingFilter(0).doFilter(new MockHttpServletRequest("GET", "/api/notifications/summary"),
                new MockHttpServletResponse(), (a, b) -> {});
        assertThat(capture.events().getFirst().getLevel()).isEqualTo(Level.INFO);
    }

    @Test
    void filterFailuresRestoreContextAndDoNotProduceADuplicateOnErrorDispatch() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/chain-failure");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MDC.put("previous", "context");
        assertThatThrownBy(() -> filter.doFilter(request, response, (a, b) -> {
            throw new IllegalStateException("private-filter-message");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(MDC.getCopyOfContextMap()).containsOnlyKeys("previous");
        assertThat(capture.events()).hasSize(1);
        assertThat(fields(capture.events().getFirst())).containsEntry("status", 500);
        assertThat(fields(capture.events().getFirst()).toString()).doesNotContain("private-filter-message");
        request.setDispatcherType(DispatcherType.ERROR);
        filter.doFilter(request, response, (a, b) -> ((HttpServletResponse) b).setStatus(500));
        assertThat(capture.events()).hasSize(1);
    }

    private Map<String, Object> fields(ILoggingEvent event) {
        return event.getKeyValuePairs().stream().collect(java.util.stream.Collectors.toMap(pair -> pair.key, pair -> pair.value));
    }

    @RestController
    static class Endpoints {
        @GetMapping("/api/logging/{value}")
        String normal(@PathVariable String value) { return "ok"; }

        @GetMapping("/api/logging-failure")
        String fail() { throw new IllegalStateException("secret-body"); }

        @GetMapping("/api/logging-business")
        String business() { throw new AuthException(AuthErrorCode.EMAIL_CODE_INVALID); }

        @GetMapping("/api/notifications/summary")
        String polling() { return "ok"; }
    }
}
