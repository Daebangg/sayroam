package com.daebangg.sayroam.global.logging;

import org.springframework.security.oauth2.client.registration.ClientRegistration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/** 공개 공급자 엔드포인트에 실제 요청한다. 클라이언트 비밀값이나 사용자 인증 정보를 전송하지 않는다. */
final class OAuthEndpointProbe implements AutoCloseable {
    private static final Map<String, URI> ENDPOINTS = Map.of(
            "google", URI.create("https://accounts.google.com/.well-known/openid-configuration"),
            "kakao", URI.create("https://kauth.kakao.com/.well-known/openid-configuration"),
            "naver", URI.create("https://nid.naver.com/oauth2.0/authorize"));

    private final long timeoutMs;
    private final Map<String, URI> endpoints;
    private final HttpClient client;
    private final JsonMapper json = JsonMapper.builder().build();

    OAuthEndpointProbe(long timeoutMs) {
        this(timeoutMs, ENDPOINTS);
    }

    OAuthEndpointProbe(long timeoutMs, Map<String, URI> endpoints) {
        this.timeoutMs = timeoutMs;
        this.endpoints = Map.copyOf(endpoints);
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER).build();
    }

    String check(String provider, ClientRegistration registration) throws IOException, InterruptedException {
        URI endpoint = endpoints.get(provider);
        if (endpoint == null) throw new IllegalArgumentException("Unsupported OAuth probe provider");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(timeoutMs)).GET().build();
        if ("naver".equals(provider)) {
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            requireReachable(response.statusCode());
            return "HTTP " + response.statusCode();
        }
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200 || response.body().length() > 65536) {
            throw new IllegalStateException("OAuth discovery response invalid");
        }
        JsonNode document = json.readTree(response.body());
        if (!registration.getProviderDetails().getAuthorizationUri().equals(document.path("authorization_endpoint").asString())
                || !registration.getProviderDetails().getTokenUri().equals(document.path("token_endpoint").asString())) {
            throw new IllegalStateException("OAuth discovery endpoints do not match the registration");
        }
        return "OIDC HTTP 200";
    }

    private void requireReachable(int status) {
        if (status < 200 || status >= 400) throw new IllegalStateException("OAuth endpoint did not respond successfully");
    }

    @Override
    public void close() {
        client.shutdownNow();
    }
}
