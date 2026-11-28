package com.daebangg.sayroam.global.logging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/** 실제 기동 검사 결과를 로컬 패널에 표시한다. OAuth 연결과 사용자 인증 성공은 구분한다. */
@Slf4j
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.logging.startup", name = "enabled", havingValue = "true", matchIfMissing = true)
public class StartupSummary {
    private static final int WIDTH = 76;
    private static final String[] LOGO = {
            "███████╗  █████╗  ██╗   ██╗ ██████╗   ██████╗   █████╗  ███╗   ███╗",
            "██╔════╝ ██╔══██╗ ╚██╗ ██╔╝ ██╔══██╗ ██╔═══██╗ ██╔══██╗ ████╗ ████║",
            "███████╗ ███████║  ╚████╔╝  ██████╔╝ ██║   ██║ ███████║ ██╔████╔██║",
            "╚════██║ ██╔══██║   ╚██╔╝   ██╔══██╗ ██║   ██║ ██╔══██║ ██║╚██╔╝██║",
            "███████║ ██║  ██║    ██║    ██║  ██║ ╚██████╔╝ ██║  ██║ ██║ ╚═╝ ██║",
            "╚══════╝ ╚═╝  ╚═╝    ╚═╝    ╚═╝  ╚═╝  ╚═════╝  ╚═╝  ╚═╝ ╚═╝     ╚═╝"
    };

    private final ConfigurableApplicationContext context;
    private final StartupChecks checks;
    private final AtomicBoolean reported = new AtomicBoolean();

    public StartupSummary(ConfigurableApplicationContext context, StartupChecks checks) {
        this.context = context;
        this.checks = checks;
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        if (event.getApplicationContext() != context || !reported.compareAndSet(false, true)) return;
        StartupChecks.Report report = checks.checkAll();
        log.atInfo().addKeyValue("event", "startup_summary").log("\n{}", render(event, report));
    }

    String render(ApplicationReadyEvent event, StartupChecks.Report report) {
        StringBuilder output = new StringBuilder();
        String heading = "─ SayRoam / Startup ";
        output.append('╭').append(heading).append("─".repeat(WIDTH - heading.length())).append('╮').append('\n');
        output.append(line("", AnsiColor.DEFAULT));
        for (String logo : LOGO) {
            String centered = " ".repeat(Math.max(0, (WIDTH - logo.length()) / 2)) + logo;
            output.append(line(centered, AnsiColor.CYAN));
        }
        output.append(line("", AnsiColor.DEFAULT));
        String port = context instanceof WebServerApplicationContext web && web.getWebServer() != null
                ? Integer.toString(web.getWebServer().getPort()) : "not started";
        output.append(line("  Mode: Development    HTTP: " + port, AnsiColor.DEFAULT));
        output.append(line("", AnsiColor.DEFAULT));
        output.append(line("  %-12s %-12s %-12s %-16s %s".formatted("SERVICE", "STACK", "CHECK", "PROBE", "TIME"), AnsiColor.DEFAULT));
        output.append(line("", AnsiColor.DEFAULT));
        for (StartupChecks.Result result : report.results()) output.append(row(result));
        output.append(line("", AnsiColor.DEFAULT));
        long totalMs = (event.getTimeTaken() == null ? 0 : event.getTimeTaken().toMillis()) + report.durationMs();
        output.append(line(String.format(Locale.ROOT, "  Startup complete / %.2fs   Checks: %.2fs", totalMs / 1000.0, report.durationMs() / 1000.0), AnsiColor.DEFAULT));
        output.append(line("  Verified = successful SQL / PING / SMTP check", AnsiColor.DEFAULT));
        output.append(line("  OAuth Reachable = server responded; user login not tested", AnsiColor.DEFAULT));
        output.append(line("", AnsiColor.DEFAULT));
        return output.append('╰').append("─".repeat(WIDTH)).append('╯').toString();
    }

    private String line(String value, AnsiColor color) {
        String text = value.length() > WIDTH ? value.substring(0, WIDTH - 3) + "..." : value;
        return "│" + AnsiOutput.toString(color, text + " ".repeat(WIDTH - text.length()), AnsiColor.DEFAULT) + "│\n";
    }

    private String row(StartupChecks.Result result) {
        String prefix = "  %-12s %-12s ".formatted(result.service(), result.stack());
        String label = switch (result.status()) {
            case VERIFIED -> "Verified";
            case REACHABLE -> "Reachable";
            case DISABLED -> "Disabled";
            case MISSING -> "Missing";
            case FAILED -> "Failed";
            case TIMEOUT -> "Timeout";
        };
        AnsiColor color = switch (result.status()) {
            case VERIFIED, REACHABLE -> AnsiColor.GREEN;
            case DISABLED -> AnsiColor.BRIGHT_BLACK;
            case MISSING, FAILED, TIMEOUT -> AnsiColor.RED;
        };
        String proof = result.proof().length() > 16 ? result.proof().substring(0, 16) : result.proof();
        String duration = result.status() == StartupChecks.Status.DISABLED ? "-" : result.durationMs() + " ms";
        String suffix = " %-16s %8s".formatted(proof, duration);
        String padded = "%-12s".formatted(label);
        return "│" + prefix + AnsiOutput.toString(color, padded, AnsiColor.DEFAULT) + suffix
                + " ".repeat(Math.max(0, WIDTH - prefix.length() - padded.length() - suffix.length())) + "│\n";
    }
}
