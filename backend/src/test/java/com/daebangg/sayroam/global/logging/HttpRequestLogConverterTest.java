package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.daebangg.sayroam.global.web.RequestLoggingFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ansi.AnsiOutput;

import static org.assertj.core.api.Assertions.assertThat;

class HttpRequestLogConverterTest {
    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Test
    void compactOutputHidesMetadataWithoutRemovingItFromTheEvent() {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class);
             LogContext ignored = LogContext.request("full-request-id")) {
            LOG.atInfo().addKeyValue("event", "http_request_completed").addKeyValue("method", "POST")
                    .addKeyValue("route", "/api/auth/refresh").addKeyValue("status", 200)
                    .addKeyValue("durationMs", 247).log("HTTP request completed");
            ILoggingEvent event = capture.events().getFirst();
            assertThat(new HttpRequestLogConverter().convert(event))
                    .isEqualTo("POST /api/auth/refresh → 200 · 247 ms")
                    .doesNotContain("requestId", "taskId", "event=", "HTTP request completed");
            assertThat(event.getMDCPropertyMap()).containsEntry("requestId", "full-request-id");
            assertThat(event.getKeyValuePairs()).hasSize(5);
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }

    @ParameterizedTest
    @CsvSource({"200,32", "304,32", "401,33", "500,31"})
    void colorsTheStatusIndependentlyOfTheLogLevel(int status, String ansiColor) {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.ALWAYS);
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class)) {
            LOG.atInfo().addKeyValue("event", "http_request_completed").addKeyValue("method", "GET")
                    .addKeyValue("route", "/api/users/me").addKeyValue("status", status)
                    .addKeyValue("durationMs", 41).log("HTTP request completed");
            String output = new HttpRequestLogConverter().convert(capture.events().getFirst());
            assertThat(output).contains("\u001B[" + ansiColor + "m" + status,
                    "\u001B[36mGET", "\u001B[34m/api/users/me", "\u001B[35m41 ms");
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }

    @Test
    void unexpectedFailureStillShowsTheSafeDiagnosticsAndRequestId() {
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class);
             LogContext ignored = LogContext.request("failure-request-id")) {
            SafeLog.atError(LOG, new IllegalStateException("private-message"))
                    .addKeyValue("event", "http_request_completed").addKeyValue("method", "GET")
                    .addKeyValue("route", "/api/users/me").addKeyValue("status", 500)
                    .addKeyValue("durationMs", 3).addKeyValue("errorCode", "INTERNAL_SERVER_ERROR")
                    .log("HTTP request completed");
            assertThat(new HttpRequestLogConverter().convert(capture.events().getFirst()))
                    .contains("INTERNAL_SERVER_ERROR", "requestId=failure-request-id", "IllegalStateException")
                    .doesNotContain("private-message");
        }
    }

    @Test
    void avoidsArtificialPaddingAndNeverTruncatesLongRoutes() {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        try (LogCapture capture = new LogCapture(RequestLoggingFilter.class)) {
            String[] routes = {"UNMAPPED", "/api/auth/social/providers", "/api/users/me", "/api/" + "long-route/".repeat(6)};
            for (String route : routes) {
                LOG.atInfo().addKeyValue("event", "http_request_completed").addKeyValue("method", "GET")
                        .addKeyValue("route", route).addKeyValue("status", 200).addKeyValue("durationMs", 1)
                        .log("HTTP request completed");
            }
            HttpRequestLogConverter converter = new HttpRequestLogConverter();
            java.util.List<String> rows = capture.events().stream().map(converter::convert).toList();
            for (int index = 0; index < routes.length; index++) {
                assertThat(rows.get(index)).isEqualTo("GET " + routes[index] + " → 200 · 1 ms");
            }
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }
}
