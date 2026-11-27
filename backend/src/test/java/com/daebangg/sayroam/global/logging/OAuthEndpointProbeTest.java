package com.daebangg.sayroam.global.logging;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthEndpointProbeTest {
    @Test
    void usesRealHttpAndChecksDiscoveryEndpointsRatherThanAcceptingAny200() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/discovery", exchange -> {
            byte[] body = ("{\"authorization_endpoint\":\"https://accounts.google.com/o/oauth2/v2/auth\","
                    + "\"token_endpoint\":\"https://oauth2.googleapis.com/token\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/invalid", exchange -> {
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        String base = "http://127.0.0.1:" + server.getAddress().getPort();
        try (OAuthEndpointProbe good = new OAuthEndpointProbe(1000, Map.of("google", URI.create(base + "/discovery")));
             OAuthEndpointProbe bad = new OAuthEndpointProbe(1000, Map.of("google", URI.create(base + "/invalid")))) {
            assertThat(good.check("google", StartupChecksTest.google())).isEqualTo("OIDC HTTP 200");
            assertThatThrownBy(() -> bad.check("google", StartupChecksTest.google()))
                    .isInstanceOf(IllegalStateException.class);
        } finally {
            server.stop(0);
        }
    }
}
