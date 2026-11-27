package com.daebangg.sayroam.global.logging;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupSummaryTest {
    @Test
    void rendersAlignedArtWithActualProbeEvidenceAndDifferentOAuthStatus() {
        AnsiOutput.Enabled previous = AnsiOutput.getEnabled();
        AnsiOutput.setEnabled(AnsiOutput.Enabled.NEVER);
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.refresh();
            StartupChecks.Report report = new StartupChecks.Report(List.of(
                    new StartupChecks.Result("Database", "MySQL", StartupChecks.Status.VERIFIED, "SELECT 1", 4),
                    new StartupChecks.Result("Cache", "Redis", StartupChecks.Status.VERIFIED, "PING / PONG", 2),
                    new StartupChecks.Result("OAuth", "Google", StartupChecks.Status.REACHABLE, "OIDC HTTP 200", 150),
                    new StartupChecks.Result("Email", "SMTP", StartupChecks.Status.DISABLED, "", 0)), 150);
            String panel = new StartupSummary(context, mock(StartupChecks.class)).render(ready(context), report);
            assertThat(panel).contains("SayRoam / Startup", "███████╗", "SELECT 1", "PING / PONG", "Reachable", "150 ms",
                    "user login not tested", "4.08s");
            assertThat(panel.lines().map(String::length)).allMatch(length -> length == 78);
        } finally {
            AnsiOutput.setEnabled(previous);
        }
    }

    @Test
    void checksAreRunOnlyOnceAndEventsFromOtherContextsAreIgnored() {
        try (GenericApplicationContext context = new GenericApplicationContext();
             GenericApplicationContext other = new GenericApplicationContext();
             LogCapture capture = new LogCapture(StartupSummary.class)) {
            context.refresh();
            StartupChecks checks = mock(StartupChecks.class);
            when(checks.checkAll()).thenReturn(new StartupChecks.Report(List.of(), 0));
            StartupSummary summary = new StartupSummary(context, checks);
            summary.onReady(ready(other));
            assertThat(capture.events()).isEmpty();
            summary.onReady(ready(context));
            summary.onReady(ready(context));
            assertThat(capture.events()).hasSize(1);
            verify(checks, times(1)).checkAll();
        }
    }

    @Test
    void thePanelIsLocalOnlyAndCanBeDisabled() {
        ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(StartupSummary.class)
                .withInitializer(context -> context.getBeanFactory().registerSingleton("startupChecks", mock(StartupChecks.class)));
        runner.run(context -> assertThat(context).doesNotHaveBean(StartupSummary.class));
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .withPropertyValues("app.logging.startup.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(StartupSummary.class));
        runner.withInitializer(context -> context.getEnvironment().setActiveProfiles("local"))
                .run(context -> assertThat(context).hasSingleBean(StartupSummary.class));
    }

    private ApplicationReadyEvent ready(ConfigurableApplicationContext context) {
        return new ApplicationReadyEvent(new SpringApplication(), new String[0], context, Duration.ofMillis(3930));
    }
}
