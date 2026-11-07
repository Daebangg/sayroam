package com.daebangg.sayroam.auth;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.global.config.SwaggerAuthIndexTransformer;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 필터·컨트롤러·유스케이스·MySQL 트랜잭션·Redis Lua를 통과한다. SMTP만 수집기로 대체한다. */
@AutoConfigureMockMvc
@Import({AuthIntegrationTests.MailCaptureConfig.class, SwaggerAuthIndexTransformer.class})
class AuthIntegrationTests extends DatabaseIntegrationTest {
    private static final String PASSWORD = "test-password-123";
    private static final String REFRESH = "sayroam_refresh";
    private static final String[] TABLES = {"expense_share", "expense", "trip_place", "trip",
            "companion_request", "companion_search", "room_invitation", "room_member", "chat_message",
            "room", "place", "user_travel_style", "travel_style_item", "notification",
            "user_social_account", "user_credential", "users"};

    @Autowired MockMvc mvc;
    @Autowired JsonMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired MailCapture mail;
    @Autowired JwtEncoder encoder;
    private Cookie csrf;

    @BeforeEach
    void prepare() throws Exception {
        clean();
        csrf = mvc.perform(get("/api/auth/csrf").servletPath("/api/auth/csrf"))
                .andExpect(status().isNoContent()).andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertThat(csrf).isNotNull();
    }

    @AfterEach
    void clean() throws Exception {
        jdbc.execute("DROP TRIGGER IF EXISTS test_fail_user_update");
        for (String table : TABLES) jdbc.update("DELETE FROM " + table);
        // CLI를 테스트 컨테이너 안에서만 실행하므로 개발 Redis에 접근할 수 없다.
        assertThat(REDIS.execInContainer("redis-cli", "FLUSHDB").getExitCode()).isZero();
        mail.codes.clear();
        mail.fail = false;
    }

    @Test
    void verifiedSignupConsumesProofOnceAndLoginIssuesOnlyAccessInBody() throws Exception {
        String email = email();
        String proof = signupProof(email);
        var body = Map.of("email", email.toUpperCase(), "password", PASSWORD, "nickname", " 여행자 ", "signupProof", proof);
        var signup = call(HttpMethod.POST, "/api/auth/signup", body, null);
        assertStatus(signup, 201);
        assertThat(signup.getResponse().getCookie(REFRESH)).isNull();
        assertStatus(call(HttpMethod.POST, "/api/auth/signup", body, null), 409);
        String hash = jdbc.queryForObject("SELECT password_hash FROM user_credential", String.class);
        assertThat(hash).isNotEqualTo(PASSWORD);
        assertThat(passwords.matches(PASSWORD, hash)).isTrue();
        Login login = login(email, PASSWORD);
        assertThat(login.refresh().isHttpOnly()).isTrue();
        assertThat(login.refresh().getPath()).isEqualTo("/api/auth");
        assertThat(login.refresh().getAttribute("SameSite")).isEqualTo("Strict");
        var me = call(HttpMethod.GET, "/api/users/me", null, login.access());
        assertStatus(me, 200);
        assertThat(json(me).path("email").asText()).isEqualTo(email);
        assertThat(json(me).path("nickname").asText()).isEqualTo("여행자");
        assertThat(me.getRequest().getSession(false)).isNull();
    }

    @Test
    void swaggerAutomaticallySuppliesCsrfButServerStillRejectsMissingOrWrongHeaders() throws Exception {
        var page = mvc.perform(get("/swagger-ui/index.html").servletPath("/swagger-ui/index.html"))
                .andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
        var initializer = mvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(initializer).contains("requestInterceptor", "document.cookie", "X-XSRF-TOKEN", "isSameOrigin");
        assertThat(initializer).contains("responseInterceptor:", "ui.preauthorizeApiKey(\"bearerAuth\"",
                "ui.authActions.logout([\"bearerAuth\"])");
        JsonNode spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        var passwordSecurity = spec.path("paths").path("/api/auth/password").path("patch").path("security");
        assertThat(passwordSecurity.size()).isEqualTo(1);
        assertThat(passwordSecurity.get(0).has("bearerAuth")).isTrue();
        assertThat(passwordSecurity.get(0).has("csrfToken")).isTrue();
        assertThat(spec.path("paths").path("/api/auth/login").path("post").path("parameters").isMissingNode()).isTrue();
        assertThat(spec.path("paths").path("/api/auth/csrf").path("get").path("parameters").isMissingNode()).isTrue();
        var logoutCookie = spec.path("paths").path("/api/auth/logout").path("post").path("parameters").get(0);
        assertThat(logoutCookie.path("name").asText()).isEqualTo("sayroam_refresh");
        assertThat(logoutCookie.path("schema").path("type").asText()).isEqualTo("string");
        for (String path : new String[]{"/api/auth/login", "/api/auth/refresh", "/api/auth/logout"}) {
            mvc.perform(post(path).servletPath(path).cookie(csrf).header("Referer", "http://localhost/swagger-ui/index.html"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).servletPath(path).header("X-XSRF-TOKEN", csrf.getValue()))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", "wrong"))
                    .andExpect(status().isForbidden());
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/logout", null, null), 204);
    }

    @Test
    void socialExchangeOpenApiAllowsNullableReferencedResponses() throws Exception {
        var spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        var properties = spec.path("components").path("schemas").path("SocialExchangeResponse").path("properties");
        for (var field : Map.of("login", "LoginResponse", "signup", "Signup").entrySet()) {
            var property = properties.path(field.getKey());
            assertThat(property.has("$ref")).isFalse();
            assertThat(property.has("type")).isFalse();
            assertThat(property.path("oneOf").size()).isEqualTo(2);
            assertThat(List.of(property.path("oneOf").get(0), property.path("oneOf").get(1)))
                    .anySatisfy(branch -> assertThat(branch.path("$ref").asText())
                            .isEqualTo("#/components/schemas/" + field.getValue()))
                    .anySatisfy(branch -> assertThat(branch.path("type").asText()).isEqualTo("null"));
            assertThat(property.path("description").asText()).isNotBlank();
        }
    }

    @Test
    void loginFailuresDoNotDiscloseWhetherAccountExistsOrHasPassword() throws Exception {
        String email = register();
        for (String address : new String[]{email, email()}) {
            var failed = call(HttpMethod.POST, "/api/auth/login", Map.of("email", address, "password", "wrong-password"), null);
            assertStatus(failed, 401);
            assertThat(json(failed).path("code").asText()).isEqualTo("INVALID_CREDENTIALS");
            assertThat(failed.getResponse().getCookie(REFRESH)).isNull();
        }
        jdbc.update("DELETE FROM user_credential");
        assertStatus(call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null), 401);
    }

    @Test
    void refreshRotatesAndReplayRevokesFamilyWhileUnknownSecretDoesNot() throws Exception {
        Login first = login(register(), PASSWORD);
        String raw = first.refresh().getValue();
        String forged = raw.substring(0, raw.indexOf('.') + 1) + (raw.endsWith("A") ? "B" : "A").repeat(43);
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, new Cookie(REFRESH, forged)), 401);
        var rotated = call(HttpMethod.POST, "/api/auth/refresh", null, "expired-or-broken", first.refresh());
        assertStatus(rotated, 200);
        Cookie next = rotated.getResponse().getCookie(REFRESH);
        assertThat(next.getValue()).isNotEqualTo(raw);
        var replay = call(HttpMethod.POST, "/api/auth/refresh", null, null, first.refresh());
        assertStatus(replay, 401);
        assertThat(json(replay).path("code").asText()).isEqualTo("REFRESH_TOKEN_REUSED");
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, next), 401);
    }

    @Test
    void concurrentRefreshHasOneWinnerAndReplayRevokesEvenTheWinningReplacement() throws Exception {
        Login login = login(register(), PASSWORD);
        var results = concurrent(() -> call(HttpMethod.POST, "/api/auth/refresh", null, null, login.refresh()));
        assertThat(results).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(200, 401);
        Cookie winner = results.stream().filter(r -> r.getResponse().getStatus() == 200).findFirst().orElseThrow()
                .getResponse().getCookie(REFRESH);
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, winner), 401);
    }

    @Test
    void logoutIsIdempotentAffectsOnlyCurrentLoginAndRetainsAlreadyIssuedAccess() throws Exception {
        String email = register();
        Login first = login(email, PASSWORD);
        Login other = login(email, PASSWORD);
        for (int i = 0; i < 2; i++) {
            var logout = call(HttpMethod.POST, "/api/auth/logout", null, "expired-or-broken", first.refresh());
            assertStatus(logout, 204);
            assertThat(logout.getResponse().getCookie(REFRESH).getMaxAge()).isZero();
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, first.refresh()), 401);
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, other.refresh()), 200);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, first.access()), 200);
    }

    @Test
    void passwordProofIsBoundToPurposeAndAccountAndChangeRevokesEveryDevice() throws Exception {
        String email = register();
        Login one = login(email, PASSWORD);
        Login two = login(email, PASSWORD);
        Login otherUser = login(register(), PASSWORD);
        String proof = accountProof(one, email, "PASSWORD_CHANGE");
        assertStatus(call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", proof), one.access()), 410);
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "new-password-456"), otherUser.access()), 410);
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "short"), one.access()), 400);
        var changed = call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "new-password-456"), one.access());
        assertStatus(changed, 204);
        assertThat(changed.getResponse().getCookie(REFRESH).getMaxAge()).isZero();
        for (Login old : new Login[]{one, two}) {
            assertStatus(call(HttpMethod.GET, "/api/users/me", null, old.access()), 401);
            assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, old.refresh()), 401);
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null), 401);
        assertThat(login(email, "new-password-456").access()).isNotBlank();
    }

    @Test
    void deletionCleansPrivateDataPreservesSharedHistoryAndAllowsNewIdentity() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        seedDeletionData(id);
        String proof = accountProof(login, email, "ACCOUNT_DELETION");
        assertStatus(call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", proof), login.access()), 204);
        assertThat(jdbc.queryForObject("SELECT email FROM users WHERE id = ?", String.class, id)).isNull();
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NOT NULL AND auth_version = 1 FROM users WHERE id = ?", Boolean.class, id)).isTrue();
        for (String table : new String[]{"user_credential", "user_social_account", "user_travel_style", "notification"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM chat_message WHERE sender_id = ?", Integer.class, id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM room_member WHERE user_id = ?", Integer.class, id)).isEqualTo(1);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, login.access()), 401);
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, login.refresh()), 401);
        // 이후 재가입 절차 검증을 위해 테스트 컨테이너의 발송 cooldown만 포함한 상태를 초기화한다.
        REDIS.execInContainer("redis-cli", "FLUSHDB");
        long newId = signup(email, signupProof(email));
        assertThat(newId).isNotEqualTo(id);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM room_member WHERE user_id = ?", Integer.class, newId)).isZero();
    }

    @Test
    void databaseFailureRollsBackAllDeletionAdaptersButDoesNotRestoreConsumedRedisProof() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        seedDeletionData(id);
        String proof = accountProof(login, email, "ACCOUNT_DELETION");
        jdbc.execute("""
                CREATE TRIGGER test_fail_user_update BEFORE UPDATE ON users FOR EACH ROW
                SIGNAL SQLSTATE '40001' SET MYSQL_ERRNO = 1213, MESSAGE_TEXT = 'test transaction failure'
                """);
        var failed = call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", proof), login.access());
        assertStatus(failed, 503);
        assertThat(json(failed).path("code").asText()).isEqualTo("ACCOUNT_DELETION_UNAVAILABLE");
        jdbc.execute("DROP TRIGGER test_fail_user_update");
        for (String table : new String[]{"user_credential", "user_social_account", "user_travel_style", "notification"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
        }
        assertThat(jdbc.queryForObject("SELECT deleted_at IS NULL AND auth_version = 0 FROM users WHERE id = ?", Boolean.class, id)).isTrue();
        assertStatus(call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", proof), login.access()), 409);
    }

    @Test
    void codeAttemptLimitAndSmtpFailureCannotIssueSignupProof() throws Exception {
        String email = email();
        var issued = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email), null);
        assertStatus(issued, 202);
        String wrong = mail.code(email, "signup").equals("000000") ? "111111" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            var result = call(HttpMethod.POST, "/api/auth/email-verifications/verify",
                    Map.of("email", email, "verificationId", json(issued).path("verificationId").asText(), "code", wrong), null);
            assertStatus(result, attempt < 5 ? 400 : 429);
        }
        mail.fail = true;
        var failed = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email()), null);
        assertStatus(failed, 502);
        assertThat(json(failed).path("code").asText()).isEqualTo("EMAIL_DELIVERY_FAILED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void concurrentSignupCanConsumeProofOnlyOnce() throws Exception {
        String email = email();
        var body = Map.of("email", email, "password", PASSWORD, "nickname", "여행자", "signupProof", signupProof(email));
        var results = concurrent(() -> call(HttpMethod.POST, "/api/auth/signup", body, null));
        assertThat(results).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "expired", "future", "type", "use", "version", "signature"})
    void bearerRejectsInvalidJwtClaimsAndSignature(String invalid) throws Exception {
        String email = register();
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().subject(Long.toString(id)).id(UUID.randomUUID().toString())
                .issuer(invalid.equals("issuer") ? "other" : "sayroam")
                .audience(java.util.List.of(invalid.equals("audience") ? "other" : "sayroam-api"))
                .issuedAt(now.minusSeconds(1000)).expiresAt(invalid.equals("expired") ? now.minusSeconds(120) : now.plusSeconds(600))
                .claim("token_use", invalid.equals("use") ? "refresh" : "access")
                .claim("auth_ver", invalid.equals("version") ? "1" : "0");
        if (invalid.equals("future")) claims.issuedAt(now.plusSeconds(120));
        var header = JwsHeader.with(MacAlgorithm.HS256).type(invalid.equals("type") ? "JWT" : "at+jwt").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        if (invalid.equals("signature")) {
            String[] parts = token.split("\\.");
            token = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        }
        var rejected = call(HttpMethod.GET, "/api/users/me", null, token);
        assertStatus(rejected, 401);
        assertThat(rejected.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void redisOutageFailsClosedWithoutClearingRefreshCookieOrBlockingAccessAuthentication() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        var docker = REDIS.getDockerClient();
        docker.pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            var failedLogin = call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null);
            assertStatus(failedLogin, 503);
            assertThat(failedLogin.getResponse().getCookie(REFRESH)).isNull();
            for (String path : new String[]{"/api/auth/refresh", "/api/auth/logout"}) {
                var failed = call(HttpMethod.POST, path, null, null, login.refresh());
                assertStatus(failed, 503);
                assertThat(json(failed).path("code").asText()).isEqualTo("AUTHENTICATION_UNAVAILABLE");
                assertThat(failed.getResponse().getCookie(REFRESH)).isNull();
            }
            assertStatus(call(HttpMethod.GET, "/api/users/me", null, login.access()), 200);
        } finally {
            docker.unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
        assertThat(REDIS.execInContainer("redis-cli", "PING").getStdout()).contains("PONG");
    }

    private String register() throws Exception {
        String email = email();
        signup(email, signupProof(email));
        return email;
    }

    private String signupProof(String email) throws Exception {
        var issued = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email), null);
        assertStatus(issued, 202);
        var confirmed = call(HttpMethod.POST, "/api/auth/email-verifications/verify",
                Map.of("email", email, "verificationId", json(issued).path("verificationId").asText(), "code", mail.code(email, "signup")), null);
        assertStatus(confirmed, 200);
        return json(confirmed).path("signupProof").asText();
    }

    private long signup(String email, String proof) throws Exception {
        var result = call(HttpMethod.POST, "/api/auth/signup", Map.of("email", email, "password", PASSWORD, "nickname", "여행자", "signupProof", proof), null);
        assertStatus(result, 201);
        return json(result).path("userId").asLong();
    }

    private Login login(String email, String password) throws Exception {
        var result = call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", password), null);
        assertStatus(result, 200);
        assertThat(json(result).has("refreshToken")).isFalse();
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        return new Login(json(result).path("accessToken").asText(), result.getResponse().getCookie(REFRESH));
    }

    private String accountProof(Login login, String email, String purpose) throws Exception {
        var issued = call(HttpMethod.POST, "/api/auth/account-verifications", Map.of("purpose", purpose), login.access());
        assertStatus(issued, 202);
        var confirmed = call(HttpMethod.POST, "/api/auth/account-verifications/verify", Map.of("purpose", purpose,
                "verificationId", json(issued).path("verificationId").asText(), "code", mail.code(email, purpose)), login.access());
        assertStatus(confirmed, 200);
        return json(confirmed).path("accountProof").asText();
    }

    private MvcResult call(HttpMethod method, String path, Object body, String access, Cookie... cookies) throws Exception {
        var builder = request(method, path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
        if (cookies.length > 0) builder.cookie(cookies);
        if (access != null) builder.header("Authorization", "Bearer " + access);
        if (body != null) builder.contentType("application/json").content(mapper.writeValueAsString(body));
        return mvc.perform(builder).andReturn();
    }

    private JsonNode json(MvcResult result) { return mapper.readTree(result.getResponse().getContentAsByteArray()); }
    private void assertStatus(MvcResult result, int expected) {
        assertThat(result.getResponse().getStatus()).as("%s %s", result.getRequest().getMethod(), result.getRequest().getRequestURI()).isEqualTo(expected);
    }
    private String email() { return UUID.randomUUID() + "@example.test"; }
    private record Login(String access, Cookie refresh) {}

    private java.util.List<MvcResult> concurrent(java.util.concurrent.Callable<MvcResult> action) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Callable<MvcResult> gated = () -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return action.call(); };
            var one = pool.submit(gated);
            var two = pool.submit(gated);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return java.util.List.of(one.get(20, TimeUnit.SECONDS), two.get(20, TimeUnit.SECONDS));
        }
    }

    private void seedDeletionData(long id) {
        jdbc.update("INSERT INTO user_social_account (user_id, provider, provider_user_id, created_at, updated_at) VALUES (?, 'GOOGLE', 'test-provider-id', NOW(), NOW())", id);
        jdbc.update("INSERT INTO travel_style_item (name, min_label, max_label, sort_order, is_active, created_at, updated_at) VALUES ('item','min','max',1,1,NOW(),NOW())");
        long item = jdbc.queryForObject("SELECT id FROM travel_style_item", Long.class);
        jdbc.update("INSERT INTO user_travel_style (user_id,item_id,score,created_at,updated_at) VALUES (?,?,3,NOW(),NOW())", id, item);
        jdbc.update("INSERT INTO notification (receiver_id,type,content,created_at,updated_at) VALUES (?,'TRIP_INVITE','private',NOW(),NOW())", id);
        jdbc.update("INSERT INTO room (created_at,updated_at) VALUES (NOW(),NOW())");
        long room = jdbc.queryForObject("SELECT id FROM room", Long.class);
        jdbc.update("INSERT INTO room_member (room_id,user_id,role,status,notification_muted,joined_at,created_at,updated_at) VALUES (?,?,'OWNER','JOINED',0,NOW(),NOW(),NOW())", room, id);
        jdbc.update("INSERT INTO chat_message (room_id,sender_id,type,content,created_at,updated_at) VALUES (?,?,'TEXT','shared history',NOW(),NOW())", room, id);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class MailCaptureConfig { @Bean MailCapture mailCapture() { return new MailCapture(); } }

    static class MailCapture implements VerificationEmailSender {
        final Map<String, String> codes = new ConcurrentHashMap<>();
        volatile boolean fail;
        private void capture(String recipient, String purpose, String code) {
            assertThat(recipient).endsWith("@example.test");
            if (fail) throw new AuthException(AuthErrorCode.EMAIL_DELIVERY_FAILED);
            codes.put(recipient + ":" + purpose, code);
        }
        String code(String recipient, String purpose) { return codes.get(recipient + ":" + purpose); }
        @Override public void sendVerificationCode(String recipient, String code) { capture(recipient, "signup", code); }
        @Override public void sendAccountVerificationCode(String recipient, String code, AccountVerificationPurpose purpose) { capture(recipient, purpose.name(), code); }
    }
}
