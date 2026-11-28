package com.daebangg.sayroam.global.logging;

import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class StartupChecksIntegrationTest extends DatabaseIntegrationTest {
    @Autowired ConfigurableApplicationContext context;

    @Test
    void verifiesRealTestcontainersMySqlAndRedisConnections() {
        try (StartupChecks checks = new StartupChecks(context, 3000, (provider, registration) -> "unused")) {
            StartupChecks.Report report = checks.checkAll();
            assertThat(report.results()).anySatisfy(result -> {
                assertThat(result.stack()).isEqualTo("MySQL");
                assertThat(result.status()).isEqualTo(StartupChecks.Status.VERIFIED);
                assertThat(result.proof()).isEqualTo("SELECT 1");
            }).anySatisfy(result -> {
                assertThat(result.stack()).isEqualTo("Redis");
                assertThat(result.status()).isEqualTo(StartupChecks.Status.VERIFIED);
                assertThat(result.proof()).isEqualTo("PING / PONG");
            });
        }
    }
}
