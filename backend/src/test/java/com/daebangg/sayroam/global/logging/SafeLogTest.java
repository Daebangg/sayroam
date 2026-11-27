package com.daebangg.sayroam.global.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SafeLogTest {
    private static final Logger LOG = LoggerFactory.getLogger(SafeLogTest.class);

    @Test
    void jsonContainsCauseTypesAndLocationsWithoutAnyRawExceptionMessages() {
        IllegalStateException failure = new IllegalStateException("password=secret-cookie",
                new IllegalArgumentException("https://example.test?serviceKey=private-key&email=private@example.test"));
        failure.addSuppressed(new RuntimeException("private-chat-content"));
        try (LogCapture capture = new LogCapture(SafeLogTest.class);
             LogContext ignored = LogContext.request("request-test")) {
            SafeLog.atWarn(LOG, failure).addKeyValue("event", "test_failure").log("Operation failed");
            ILoggingEvent event = capture.events().getFirst();
            LoggerContext context = new LoggerContext();
            context.putObject(Environment.class.getName(), new MockEnvironment());
            StructuredLogEncoder encoder = new StructuredLogEncoder();
            encoder.setContext(context);
            encoder.setFormat("logstash");
            encoder.setCharset(StandardCharsets.UTF_8);
            encoder.start();
            try {
                String json = new String(encoder.encode(event), StandardCharsets.UTF_8);
                assertThat(json).contains("IllegalStateException", "IllegalArgumentException", "SafeLogTest",
                                "request-test", "exceptionFrames", "test_failure")
                        .doesNotContain("secret-cookie", "private-key", "private@example.test", "private-chat-content");
                assertThat(event.getThrowableProxy()).isNull();
            } finally {
                encoder.stop();
                context.stop();
            }
        }
    }

    @Test
    void circularAndDeepCauseChainsAreBounded() {
        RuntimeException first = new RuntimeException("secret-1");
        RuntimeException second = new RuntimeException("secret-2", first);
        first.initCause(second);
        try (LogCapture capture = new LogCapture(SafeLogTest.class)) {
            SafeLog.atWarn(LOG, first).log("Failure");
            assertThat(capture.events()).hasSize(1);
            assertThat(capture.events().getFirst().getKeyValuePairs().toString())
                    .doesNotContain("secret-1", "secret-2").hasSizeLessThan(5000);
        }
    }
}
