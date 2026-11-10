package com.daebangg.sayroam.auth;

import com.daebangg.sayroam.global.mail.EmailLanguage;
import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialAuthTicketStore;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.domain.AccountVerificationPurpose;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.VerificationDigests;
import com.daebangg.sayroam.auth.presentation.cookie.OAuthBrowserCookie;
import com.daebangg.sayroam.global.config.SwaggerAuthIndexTransformer;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import com.github.dockerjava.api.DockerClient;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.Container;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Duration;
import java.time.Instant;
import java.time.Year;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 필터·컨트롤러·유스케이스·MySQL 트랜잭션·Redis Lua를 통과한다. SMTP만 수집기로 대체한다. */
@AutoConfigureMockMvc
@Import({AuthIntegrationTests.MailCaptureConfig.class, SwaggerAuthIndexTransformer.class})
class AuthIntegrationTests extends DatabaseIntegrationTest {
    private static final String PASSWORD = "Test-password-123";
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
    @Autowired SocialAuthTicketStore socialTickets;
    @Autowired OAuthBrowserCookie oauthBrowserCookie;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
    @Autowired com.daebangg.sayroam.auth.application.port.out.AccountRegistration registrations;
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
        mail.languages.clear();
        mail.fail = false;
    }

    @Test
    void verifiedSignupConsumesProofOnceAndLoginIssuesOnlyAccessInBody() throws Exception {
        String email = email();
        String proof = signupProof(email);
        Map<String, String> body = Map.of("email", email.toUpperCase(), "password", PASSWORD, "nickname", " 여행자 ", "signupProof", proof);
        MvcResult signup = call(HttpMethod.POST, "/api/auth/signup", body, null);
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
        MvcResult me = call(HttpMethod.GET, "/api/users/me", null, login.access());
        assertStatus(me, 200);
        assertThat(json(me).path("email").asText()).isEqualTo(email);
        assertThat(json(me).path("nickname").asText()).isEqualTo("여행자");
        assertThat(me.getRequest().getSession(false)).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\n　 ", "https://example.com/profile.png", " https://example.com/profile.png "})
    void socialSignupPersistsOptionalImagesAndConsumesTicketWithoutEmailProof(String imageUrl) throws Exception {
        String email = email();
        String ticket = OAuthSecrets.create();
        String browserBinding = OAuthSecrets.create();
        SocialIdentity identity = new SocialIdentity(SocialProvider.NAVER, "profile-user", email,
                true, "공급자 닉네임", imageUrl);
        assertThat(socialTickets.saveSignup(VerificationDigests.sha256(ticket),
                VerificationDigests.sha256(browserBinding), identity, Duration.ofMinutes(5))).isTrue();
        Cookie browser = new Cookie(oauthBrowserCookie.issue(browserBinding).getName(), browserBinding);
        Map<String, String> body = Map.of("signupTicket", ticket);

        MvcResult signup = call(HttpMethod.POST, "/api/auth/social/signup", body, null, browser);
        assertStatus(signup, 200);
        assertThat(signup.getResponse().getCookie(REFRESH)).isNotNull();
        String access = json(signup).path("accessToken").asText();
        assertThat(access).isNotBlank();
        MvcResult me = call(HttpMethod.GET, "/api/users/me", null, access);
        assertStatus(me, 200);
        assertThat(json(me).path("email").asText()).isEqualTo(email);
        String expectedImage = imageUrl == null || imageUrl.isBlank() ? null : imageUrl.strip();
        assertThat(jdbc.queryForObject("SELECT nickname FROM users WHERE email = ?", String.class, email))
                .isEqualTo("공급자 닉네임");
        assertThat(jdbc.queryForObject("SELECT profile_image_url FROM users WHERE email = ?", String.class, email))
                .isEqualTo(expectedImage);
        assertThat(json(me).path("profileImageUrl").isNull()).isEqualTo(expectedImage == null);
        if (expectedImage != null) {
            assertThat(json(me).path("profileImageUrl").asText()).isEqualTo(expectedImage);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_social_account WHERE provider = 'NAVER' "
                + "AND provider_user_id = 'profile-user' AND user_id = ?", Integer.class,
                json(me).path("userId").asLong())).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_credential", Integer.class)).isZero();

        MvcResult replay = call(HttpMethod.POST, "/api/auth/social/signup", body, null, browser);
        assertStatus(replay, 400);
        assertThat(json(replay).path("code").asText()).isEqualTo("SOCIAL_AUTH_INVALID");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @Test
    void profileLifecyclePersistsPartialChangesAndDeletionRevokesAccess() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        Login other = login(register(), PASSWORD);
        JsonNode otherBefore = json(call(HttpMethod.GET, "/api/users/me", null, other.access()));
        MvcResult initial = call(HttpMethod.GET, "/api/users/me", null, login.access());
        assertStatus(initial, 200);
        long id = json(initial).path("userId").asLong();
        for (String field : new String[]{"profileImageUrl", "gender", "birthYear"}) {
            assertThat(json(initial).has(field)).as(field).isTrue();
            assertThat(json(initial).path(field).isNull()).as(field).isTrue();
        }
        assertThat(json(initial).path("role").asText()).isEqualTo("USER");
        assertThat(jdbc.queryForObject("SELECT email_verified_at IS NOT NULL FROM users WHERE id = ?",
                Boolean.class, id)).isTrue();

        MvcResult changed = call(HttpMethod.PATCH, "/api/users/me", Map.of(
                "nickname", "  제주 여행자  ",
                "profileImageUrl", " https://example.com/profile.png ",
                "gender", "MALE", "birthYear", 1990), login.access());
        assertStatus(changed, 200);
        assertThat(changed.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(json(changed).path("nickname").asText()).isEqualTo("제주 여행자");
        assertThat(json(changed).path("profileImageUrl").asText()).isEqualTo("https://example.com/profile.png");
        assertThat(json(changed).path("gender").asText()).isEqualTo("MALE");
        assertThat(json(changed).path("birthYear").asInt()).isEqualTo(1990);
        assertThat(json(call(HttpMethod.GET, "/api/users/me", null, login.access()))).isEqualTo(json(changed));
        assertThat(jdbc.queryForObject("SELECT birth_year FROM users WHERE id = ?", Integer.class, id))
                .isEqualTo(1990);

        MvcResult nicknameOnly = call(HttpMethod.PATCH, "/api/users/me", Map.of("nickname", "다음 여행자"), login.access());
        assertStatus(nicknameOnly, 200);
        assertThat(json(nicknameOnly).path("gender").asText()).isEqualTo("MALE");
        assertThat(json(nicknameOnly).path("birthYear").asInt()).isEqualTo(1990);
        assertThat(json(nicknameOnly).path("profileImageUrl").asText()).isEqualTo("https://example.com/profile.png");
        assertStatus(call(HttpMethod.PATCH, "/api/users/me", Map.of("gender", "FEMALE"), login.access()), 200);

        for (String field : new String[]{"gender", "birthYear", "profileImageUrl"}) {
            MvcResult cleared = call(HttpMethod.PATCH, "/api/users/me", profileField(field, null), login.access());
            assertStatus(cleared, 200);
            assertThat(json(cleared).has(field)).as(field).isTrue();
            assertThat(json(cleared).path(field).isNull()).as(field).isTrue();
            assertThat(json(cleared).path("nickname").asText()).isEqualTo("다음 여행자");
        }
        MvcResult emptyPatch = call(HttpMethod.PATCH, "/api/users/me", Map.of(), login.access());
        assertStatus(emptyPatch, 200);
        assertThat(json(call(HttpMethod.GET, "/api/users/me", null, login.access()))).isEqualTo(json(emptyPatch));
        assertThat(json(call(HttpMethod.GET, "/api/users/me", null, other.access()))).isEqualTo(otherBefore);
        assertThat(jdbc.queryForObject("SELECT auth_version FROM users WHERE id = ?", Long.class, id)).isZero();

        assertStatus(call(HttpMethod.PATCH, "/api/users/me", Map.of("profileImageUrl",
                "https://example.com/final.png", "gender", "FEMALE", "birthYear", 2000), login.access()), 200);
        String proof = accountProof(login, email, "ACCOUNT_DELETION");
        assertStatus(call(HttpMethod.POST, "/api/auth/account-deletion",
                Map.of("accountProof", proof), login.access()), 204);
        assertThat(jdbc.queryForObject("""
                SELECT deleted_at IS NOT NULL AND email IS NULL AND profile_image_url IS NULL
                    AND gender IS NULL AND birth_year IS NULL FROM users WHERE id = ?
                """, Boolean.class, id)).isTrue();
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, login.access()), 401);
        assertStatus(call(HttpMethod.PATCH, "/api/users/me", Map.of("nickname", "복원 시도"), login.access()), 401);
        assertStatus(call(HttpMethod.POST, "/api/auth/login",
                Map.of("email", email, "password", PASSWORD), null), 401);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, other.access()), 200);
    }

    @Test
    void invalidProfileInputsReturnProblemsAndDoNotPersistPartialChanges() throws Exception {
        Login login = login(register(), PASSWORD);
        JsonNode before = json(call(HttpMethod.GET, "/api/users/me", null, login.access()));
        record InvalidInput(Object body, String code) {}
        List<InvalidInput> invalidInputs = List.of(
                new InvalidInput(profileField("nickname", null), "USER_NICKNAME_REQUIRED"),
                new InvalidInput(profileField("nickname", "  "), "USER_NICKNAME_REQUIRED"),
                new InvalidInput(profileField("nickname", "가".repeat(51)), "USER_NICKNAME_TOO_LONG"),
                new InvalidInput(profileField("nickname", "여행\n자"), "USER_NICKNAME_INVALID"),
                new InvalidInput(profileField("nickname", 123), null),
                new InvalidInput(profileField("profileImageUrl", "ftp://example.com/image"), "USER_PROFILE_IMAGE_URL_INVALID"),
                new InvalidInput(profileField("profileImageUrl", "https://example.com/" + "a".repeat(500)),
                        "USER_PROFILE_IMAGE_URL_TOO_LONG"),
                new InvalidInput(profileField("profileImageUrl", false), null),
                new InvalidInput(profileField("gender", "OTHER"), null),
                new InvalidInput(profileField("gender", 0), null),
                new InvalidInput(profileField("birthYear", 0), "USER_BIRTH_YEAR_INVALID"),
                new InvalidInput(profileField("birthYear", -1), "USER_BIRTH_YEAR_INVALID"),
                new InvalidInput(profileField("birthYear", Year.now().getValue() + 1), "USER_BIRTH_YEAR_INVALID"),
                new InvalidInput(profileField("birthYear", "2000"), null),
                new InvalidInput(profileField("birthYear", 2000.5), null),
                new InvalidInput(profileField("birthYear", 2147483648L), null),
                new InvalidInput(List.of("not-an-object"), null),
                new InvalidInput("not-an-object", null),
                new InvalidInput(mapper.readTree("null"), null),
                new InvalidInput(Map.of("nickname", "저장되면 안 됨", "birthYear", 0), "USER_BIRTH_YEAR_INVALID")
        );
        for (InvalidInput input : invalidInputs) {
            MvcResult failed = call(HttpMethod.PATCH, "/api/users/me", input.body(), login.access());
            assertStatus(failed, 400);
            assertThat(failed.getResponse().getContentType()).contains("application/problem+json");
            assertThat(json(failed).path("code").asText()).isNotBlank();
            if (input.code() != null) {
                assertThat(json(failed).path("code").asText()).isEqualTo(input.code());
            }
            assertThat(json(call(HttpMethod.GET, "/api/users/me", null, login.access())))
                    .as("unchanged after %s", input.body()).isEqualTo(before);
        }
    }

    @Test
    void protectedFieldsCannotChangeIdentityOrAnotherMembersProfile() throws Exception {
        Login login = login(register(), PASSWORD);
        Login other = login(register(), PASSWORD);
        JsonNode before = json(call(HttpMethod.GET, "/api/users/me", null, login.access()));
        JsonNode otherBefore = json(call(HttpMethod.GET, "/api/users/me", null, other.access()));
        for (String field : new String[]{"userId", "id", "email", "role", "emailVerifiedAt", "deletedAt", "authVersion"}) {
            Map<String, Object> body = profileField(field, otherBefore.path("userId").asLong());
            body.put("nickname", "권한 없는 변경");
            assertStatus(call(HttpMethod.PATCH, "/api/users/me", body, login.access()), 400);
        }
        MvcResult withForeignId = mvc.perform(get("/api/users/me").servletPath("/api/users/me")
                .param("userId", otherBefore.path("userId").asText())
                .header("Authorization", "Bearer " + login.access())).andReturn();
        assertStatus(withForeignId, 200);
        assertThat(json(withForeignId)).isEqualTo(before);
        assertThat(json(call(HttpMethod.GET, "/api/users/me", null, other.access()))).isEqualTo(otherBefore);
    }

    @Test
    void unauthenticatedAndRevokedLoginsCannotReadOrUpdateProfiles() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        for (HttpMethod method : new HttpMethod[]{HttpMethod.GET, HttpMethod.PATCH}) {
            Object body = method == HttpMethod.PATCH ? Map.of("nickname", "인증 없는 변경") : null;
            MvcResult anonymous = call(method, "/api/users/me", body, null);
            assertStatus(anonymous, 401);
            assertThat(anonymous.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(anonymous.getResponse().getHeader("Cache-Control")).contains("no-store");
        }
        jdbc.update("UPDATE users SET auth_version = auth_version + 1 WHERE email = ?", email);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, login.access()), 401);
        assertStatus(call(HttpMethod.PATCH, "/api/users/me", Map.of("nickname", "폐기된 변경"), login.access()), 401);
        assertThat(jdbc.queryForObject("SELECT nickname FROM users WHERE email = ?", String.class, email))
                .isEqualTo("여행자");
    }

    @Test
    void concurrentProfilePatchesPreserveChangesToDifferentFields() throws Exception {
        Login login = login(register(), PASSWORD);
        AtomicInteger order = new AtomicInteger();
        List<MvcResult> results = concurrent(() -> call(HttpMethod.PATCH, "/api/users/me",
                order.getAndIncrement() == 0 ? Map.of("nickname", "동시 여행자")
                        : Map.of("gender", "FEMALE", "birthYear", 1995), login.access()));
        assertThat(results).extracting(result -> result.getResponse().getStatus()).containsOnly(200);
        JsonNode profile = json(call(HttpMethod.GET, "/api/users/me", null, login.access()));
        assertThat(profile.path("nickname").asText()).isEqualTo("동시 여행자");
        assertThat(profile.path("gender").asText()).isEqualTo("FEMALE");
        assertThat(profile.path("birthYear").asInt()).isEqualTo(1995);
    }

    @Test
    void profileOpenApiIncludesNullableFieldsSecurityAndUserErrors() throws Exception {
        JsonNode spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        JsonNode operations = spec.path("paths").path("/api/users/me");
        for (String method : new String[]{"get", "patch"}) {
            JsonNode security = operations.path(method).path("security");
            assertThat(security.size()).isEqualTo(1);
            assertThat(security.get(0).has("bearerAuth")).isTrue();
            assertThat(security.get(0).has("csrfToken")).isFalse();
            assertThat(operations.path(method).path("responses").path("200")
                    .path("content").has("application/json")).isTrue();
        }
        assertThat(operations.path("patch").path("responses").path("400").path("$ref").asText())
                .isEqualTo("#/components/responses/UserProfileBadRequest");
        JsonNode schemas = spec.path("components").path("schemas");
        for (String schema : new String[]{"CurrentUserResponse", "UpdateUserProfileRequest"}) {
            JsonNode gender = schemas.path(schema).path("properties").path("gender");
            assertThat(gender.path("oneOf").size()).as(schema).isEqualTo(2);
            assertThat(List.of(gender.path("oneOf").get(0), gender.path("oneOf").get(1)))
                    .anySatisfy(branch -> assertThat(branch.path("$ref").asText()).isEqualTo("#/components/schemas/Gender"))
                    .anySatisfy(branch -> assertThat(branch.path("type").asText()).isEqualTo("null"));
        }
        JsonNode requestSchema = schemas.path("UpdateUserProfileRequest");
        assertThat(requestSchema.path("additionalProperties").asBoolean()).isFalse();
        assertThat(requestSchema.path("properties").has("providedFields")).isFalse();
        for (UserErrorCode code : UserErrorCode.values()) {
            assertThat(schemas.path("ApiErrorCode").path("enum").toString()).contains(code.getCode());
        }
        assertThat(spec.path("paths").path("/api/rooms/{roomId}/messages").path("get")
                .path("responses").path("200").path("content").has("application/json")).isTrue();
    }

    private static Map<String, Object> profileField(String field, Object value) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(field, value);
        return body;
    }

    @Test
    void swaggerAutomaticallySuppliesCsrfButServerStillRejectsMissingOrWrongHeaders() throws Exception {
        MvcResult page = mvc.perform(get("/swagger-ui/index.html").servletPath("/swagger-ui/index.html"))
                .andExpect(status().isOk()).andReturn();
        assertThat(page.getResponse().getCookie("XSRF-TOKEN")).isNotNull();
        String initializer = mvc.perform(get("/swagger-ui/swagger-initializer.js"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(initializer).contains("requestInterceptor", "document.cookie", "X-XSRF-TOKEN", "isSameOrigin");
        assertThat(initializer).contains("responseInterceptor:", "ui.preauthorizeApiKey(\"bearerAuth\"",
                "ui.authActions.logout([\"bearerAuth\"])");
        JsonNode spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        JsonNode passwordSecurity = spec.path("paths").path("/api/auth/password").path("patch").path("security");
        assertThat(passwordSecurity.size()).isEqualTo(1);
        assertThat(passwordSecurity.get(0).has("bearerAuth")).isTrue();
        assertThat(passwordSecurity.get(0).has("csrfToken")).isTrue();
        assertThat(spec.path("paths").path("/api/auth/login").path("post").path("parameters").isMissingNode()).isTrue();
        assertThat(spec.path("paths").path("/api/auth/csrf").path("get").path("parameters").isMissingNode()).isTrue();
        JsonNode logoutCookie = spec.path("paths").path("/api/auth/logout").path("post").path("parameters").get(0);
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
        JsonNode spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        JsonNode properties = spec.path("components").path("schemas").path("SocialExchangeResponse").path("properties");
        for (Map.Entry<String, String> field : Map.of("login", "LoginResponse", "signup", "Signup").entrySet()) {
            JsonNode property = properties.path(field.getKey());
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
            MvcResult failed = call(HttpMethod.POST, "/api/auth/login", Map.of("email", address, "password", "wrong-password"), null);
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
        MvcResult rotated = call(HttpMethod.POST, "/api/auth/refresh", null, "expired-or-broken", first.refresh());
        assertStatus(rotated, 200);
        Cookie next = rotated.getResponse().getCookie(REFRESH);
        assertThat(next.getValue()).isNotEqualTo(raw);
        MvcResult replay = call(HttpMethod.POST, "/api/auth/refresh", null, null, first.refresh());
        assertStatus(replay, 401);
        assertThat(json(replay).path("code").asText()).isEqualTo("REFRESH_TOKEN_REUSED");
        assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, next), 401);
    }

    @Test
    void concurrentRefreshHasOneWinnerAndReplayRevokesEvenTheWinningReplacement() throws Exception {
        Login login = login(register(), PASSWORD);
        List<MvcResult> results = concurrent(() -> call(HttpMethod.POST, "/api/auth/refresh", null, null, login.refresh()));
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
            MvcResult logout = call(HttpMethod.POST, "/api/auth/logout", null, "expired-or-broken", first.refresh());
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
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "New-password-456!"), otherUser.access()), 410);
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "short"), one.access()), 400);
        MvcResult changed = call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "New-password-456!"), one.access());
        assertStatus(changed, 204);
        assertThat(changed.getResponse().getCookie(REFRESH).getMaxAge()).isZero();
        for (Login old : new Login[]{one, two}) {
            assertStatus(call(HttpMethod.GET, "/api/users/me", null, old.access()), 401);
            assertStatus(call(HttpMethod.PATCH, "/api/users/me", Map.of("nickname", "폐기된 변경"), old.access()), 401);
            assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, old.refresh()), 401);
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null), 401);
        assertThat(login(email, "New-password-456!").access()).isNotBlank();
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
        MvcResult failed = call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", proof), login.access());
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
        MvcResult issued = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email), null);
        assertStatus(issued, 202);
        String wrong = mail.code(email, "signup").equals("000000") ? "111111" : "000000";
        for (int attempt = 1; attempt <= 5; attempt++) {
            MvcResult result = call(HttpMethod.POST, "/api/auth/email-verifications/verify",
                    Map.of("email", email, "verificationId", json(issued).path("verificationId").asText(), "code", wrong), null);
            assertStatus(result, attempt < 5 ? 400 : 429);
        }
        mail.fail = true;
        MvcResult failed = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email()), null);
        assertStatus(failed, 502);
        assertThat(json(failed).path("code").asText()).isEqualTo("EMAIL_DELIVERY_FAILED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isZero();
    }

    @Test
    void concurrentSignupCanConsumeProofOnlyOnce() throws Exception {
        String email = email();
        Map<String, String> body = Map.of("email", email, "password", PASSWORD, "nickname", "여행자", "signupProof", signupProof(email));
        List<MvcResult> results = concurrent(() -> call(HttpMethod.POST, "/api/auth/signup", body, null));
        assertThat(results).extracting(r -> r.getResponse().getStatus()).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"issuer", "audience", "expired", "future", "type", "use", "version", "signature"})
    void bearerRejectsInvalidJwtClaimsAndSignature(String invalid) throws Exception {
        String email = register();
        long id = jdbc.queryForObject("SELECT id FROM users WHERE email = ?", Long.class, email);
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject(Long.toString(id)).id(UUID.randomUUID().toString())
                .issuer(invalid.equals("issuer") ? "other" : "sayroam")
                .audience(java.util.List.of(invalid.equals("audience") ? "other" : "sayroam-api"))
                .issuedAt(now.minusSeconds(1000)).expiresAt(invalid.equals("expired") ? now.minusSeconds(120) : now.plusSeconds(600))
                .claim("token_use", invalid.equals("use") ? "refresh" : "access")
                .claim("auth_ver", invalid.equals("version") ? "1" : "0");
        if (invalid.equals("future")) claims.issuedAt(now.plusSeconds(120));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type(invalid.equals("type") ? "JWT" : "at+jwt").build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        if (invalid.equals("signature")) {
            String[] parts = token.split("\\.");
            token = parts[0] + "." + parts[1] + "." + (parts[2].startsWith("A") ? "B" : "A") + parts[2].substring(1);
        }
        MvcResult rejected = call(HttpMethod.GET, "/api/users/me", null, token);
        assertStatus(rejected, 401);
        assertThat(rejected.getResponse().getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    void redisOutageFailsClosedWithoutClearingRefreshCookieOrBlockingAccessAuthentication() throws Exception {
        String email = register();
        Login login = login(email, PASSWORD);
        DockerClient docker = REDIS.getDockerClient();
        docker.pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            MvcResult failedLogin = call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null);
            assertStatus(failedLogin, 503);
            assertThat(failedLogin.getResponse().getCookie(REFRESH)).isNull();
            for (String path : new String[]{"/api/auth/refresh", "/api/auth/logout"}) {
                MvcResult failed = call(HttpMethod.POST, path, null, null, login.refresh());
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

    @Test
    void passwordResetWorksWithoutLoginAndRevokesEveryPreviousLogin() throws Exception {
        String email = register();
        Login first = login(email, PASSWORD);
        Login second = login(email, PASSWORD);
        String proof = resetProof(email.toUpperCase());
        String replacement = "Replacement-pass1!";
        MvcResult reset = reset(email, proof, replacement);
        assertStatus(reset, 204);
        assertThat(reset.getResponse().getCookie(REFRESH).getMaxAge()).isZero();
        assertThat(reset.getResponse().getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(reset.getResponse().getContentAsString()).isEmpty();
        assertThat(reset.getRequest().getSession(false)).isNull();
        for (Login prior : java.util.List.of(first, second)) {
            assertStatus(call(HttpMethod.GET, "/api/users/me", null, prior.access()), 401);
            assertStatus(call(HttpMethod.POST, "/api/auth/refresh", null, null, prior.refresh()), 401);
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", PASSWORD), null), 401);
        login(email, replacement);
        assertThat(jdbc.queryForObject("SELECT auth_version FROM users WHERE email = ?", Long.class, email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT password_changed_at IS NOT NULL FROM user_credential", Boolean.class)).isTrue();
        assertStatus(reset(email, proof, "another-password"), 400);
    }

    @Test
    void resetIssuanceAndMailFailuresDoNotDisclosePasswordAccountEligibility() throws Exception {
        String regular = register();
        String socialOnly = register();
        jdbc.update("DELETE FROM user_credential WHERE user_id = (SELECT id FROM users WHERE email = ?)", socialOnly);
        for (String email : java.util.List.of(regular, socialOnly, email())) {
            MvcResult issued = requestReset(email);
            assertStatus(issued, 202);
            assertThat(json(issued).properties()).extracting(Map.Entry::getKey)
                    .containsExactlyInAnyOrder("verificationId", "expiresInSeconds", "resendAfterSeconds");
            assertThat(json(issued).path("expiresInSeconds").asLong()).isBetween(295L, 300L);
            assertThat(mail.code(email, "PASSWORD_RESET")).matches("[0-9]{6}");
            assertStatus(requestReset(email), 429);
        }
        mail.fail = true;
        for (String email : java.util.List.of(regular, socialOnly, email())) {
            redisReset("DEL", "cooldown", email);
            MvcResult failure = requestReset(email);
            assertStatus(failure, 502);
            assertThat(json(failure).path("code").asText()).isEqualTo("EMAIL_DELIVERY_FAILED");
            assertThat(redisReset("EXISTS", "state", email)).isEqualTo("0");
            assertStatus(requestReset(email), 429);
        }
    }

    @Test
    void resetNeverCreatesCredentialsForSocialOnlyMissingOrDeletedAccounts() throws Exception {
        String socialOnly = register();
        String deleted = register();
        jdbc.update("DELETE FROM user_credential WHERE user_id = (SELECT id FROM users WHERE email = ?)", socialOnly);
        jdbc.update("UPDATE users SET deleted_at = NOW() WHERE email = ?", deleted);
        for (String email : java.util.List.of(socialOnly, deleted, email())) {
            MvcResult rejected = reset(email, resetProof(email), "Replacement-pass1!");
            assertStatus(rejected, 400);
            assertThat(json(rejected).path("code").asText()).isEqualTo("PASSWORD_RESET_PROOF_INVALID");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM user_credential", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT auth_version FROM users WHERE email = ?", Long.class, socialOnly)).isZero();
    }

    @Test
    void resetProofCannotBeReplacedWithSignupAccountOrAnotherEmailsProof() throws Exception {
        String email = email();
        String signup = signupProof(email);
        signup(email, signup);
        Login login = login(email, PASSWORD);
        String change = accountProof(login, email, "PASSWORD_CHANGE");
        String reset = resetProof(email);
        for (String wrongPurpose : java.util.List.of(signup, change)) {
            assertStatus(reset(email, wrongPurpose, "Replacement-pass1!"), 400);
        }
        assertStatus(reset(email(), reset, "Replacement-pass1!"), 410);
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", reset, "newPassword", "Replacement-pass1!"), login.access()), 400);
        assertStatus(call(HttpMethod.POST, "/api/auth/account-deletion", Map.of("accountProof", reset), login.access()), 410);
        assertStatus(call(HttpMethod.POST, "/api/auth/signup", Map.of("email", email, "signupProof", reset,
                "password", PASSWORD, "nickname", "여행자"), null), 400);
        assertStatus(reset(email, reset, "Replacement-pass1!"), 204);
    }

    @Test
    void resetPreservesProofOnInvalidPasswordAndRequiresRealCsrfForEveryStep() throws Exception {
        String email = register();
        String proof = resetProof(email);
        Map<String, String> resetBody = Map.of("email", email, "resetProof", proof, "newPassword", "Replacement-pass1!");
        Map<String, Map<String, String>> bodies = Map.of("/api/auth/password-reset-verifications", Map.of("email", email),
                "/api/auth/password-reset-verifications/verify", Map.of("email", email, "verificationId", UUID.randomUUID().toString(), "code", "000000"),
                "/api/auth/password-reset", resetBody);
        for (Map.Entry<String, Map<String, String>> entry : bodies.entrySet()) {
            for (String wrong : java.util.List.of("", "wrong-token")) {
                MockHttpServletRequestBuilder builder = post(entry.getKey()).servletPath(entry.getKey()).contentType("application/json")
                        .content(mapper.writeValueAsString(entry.getValue()));
                if (!wrong.isEmpty()) builder.cookie(csrf).header("X-XSRF-TOKEN", wrong);
                mvc.perform(builder).andExpect(status().isForbidden());
            }
        }
        assertStatus(reset(email, proof, "short"), 400);
        assertStatus(reset(email, proof, "        "), 400);
        assertStatus(reset(email, proof, "Replacement-pass1!"), 204);
    }

    @Test
    void resetResendReplacesCodeAndProofAndKeepsEmailRateLimit() throws Exception {
        String email = register();
        MvcResult first = requestReset(email);
        Map<String, String> oldConfirmation = resetConfirmation(email, first);
        MvcResult previous = call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", oldConfirmation, null);
        assertStatus(previous, 200);
        redisReset("DEL", "cooldown", email);
        MvcResult second = requestReset(email);
        assertStatus(second, 202);
        assertStatus(reset(email, json(previous).path("resetProof").asText(), "Replacement-pass1!"), 410);
        MvcResult replaced = call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", oldConfirmation, null);
        assertStatus(replaced, 409);
        assertThat(json(replaced).path("code").asText()).isEqualTo("EMAIL_REQUEST_SUPERSEDED");
        MvcResult current = call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", resetConfirmation(email, second), null);
        assertStatus(current, 200);
        assertThat(redisReset("GET", "email-limit", email)).isEqualTo("2");
        assertStatus(call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", resetConfirmation(email, second), null), 409);
        assertStatus(reset(email, json(current).path("resetProof").asText(), "Replacement-pass1!"), 204);
    }

    @Test
    void resetCodeAndProofExpireAndWrongCodesLockTheRequest() throws Exception {
        String email = register();
        MvcResult issued = requestReset(email);
        Map<String, String> confirmation = resetConfirmation(email, issued);
        String correct = confirmation.get("code");
        for (int attempt = 1; attempt <= 5; attempt++) {
            MvcResult wrong = call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify",
                    Map.of("email", email, "verificationId", confirmation.get("verificationId"), "code", correct.equals("000000") ? "111111" : "000000"), null);
            assertStatus(wrong, attempt == 5 ? 429 : 400);
        }
        assertStatus(call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", confirmation, null), 429);
        redisReset("PEXPIREAT", "state", email, "1");
        assertStatus(call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", confirmation, null), 410);
        redisReset("DEL", "cooldown", email);
        String proof = resetProof(email);
        assertThat(Long.parseLong(redisReset("PTTL", "proof", email))).isBetween(1L, 300_000L);
        redisReset("PEXPIREAT", "proof", email, "1");
        MvcResult expired = reset(email, proof, "Replacement-pass1!");
        assertStatus(expired, 410);
        assertThat(json(expired).path("code").asText()).isEqualTo("PASSWORD_RESET_PROOF_EXPIRED");
    }

    @Test
    void resetEmailAndIpLimitsApplyWithoutAnExistingAccount() throws Exception {
        String email = email();
        assertStatus(requestReset(email), 202);
        MvcResult cooldown = requestReset(email);
        assertStatus(cooldown, 429);
        assertThat(cooldown.getResponse().getHeader("Retry-After")).isNotBlank();
        redisReset("DEL", "cooldown", email);
        redisReset("SET", "email-limit", email, "5", "PX", "3600000");
        assertStatus(requestReset(email), 429);
        String ipKey = "sayroam:auth:{password-reset}:ip-limit:" + VerificationDigests.sha256("127.0.0.1");
        assertThat(REDIS.execInContainer("redis-cli", "SET", ipKey, "20", "PX", "3600000").getExitCode()).isZero();
        assertStatus(requestReset(email()), 429);
    }

    @Test
    void concurrentResetHasOneWinnerAndCannotRestoreTheOldLogin() throws Exception {
        String email = register();
        Login original = login(email, PASSWORD);
        String proof = resetProof(email);
        List<MvcResult> results = concurrent(() -> reset(email, proof, "Replacement-pass1!"));
        assertThat(results).filteredOn(result -> result.getResponse().getStatus() == 204).hasSize(1);
        assertThat(results).filteredOn(result -> result.getResponse().getStatus() == 400 || result.getResponse().getStatus() == 409).hasSize(1);
        assertThat(jdbc.queryForObject("SELECT auth_version FROM users WHERE email = ?", Long.class, email)).isEqualTo(1);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, original.access()), 401);
        login(email, "Replacement-pass1!");
    }

    @Test
    void resetDatabaseFailureRollsBackPasswordAndVersionWithoutRestoringProof() throws Exception {
        String email = register();
        Login original = login(email, PASSWORD);
        String proof = resetProof(email);
        jdbc.execute("""
                CREATE TRIGGER test_fail_user_update BEFORE UPDATE ON users FOR EACH ROW
                SIGNAL SQLSTATE '40001' SET MYSQL_ERRNO = 1213, MESSAGE_TEXT = 'test transaction failure'
                """);
        MvcResult failed = reset(email, proof, "Replacement-pass1!");
        assertStatus(failed, 503);
        assertThat(json(failed).path("code").asText()).isEqualTo("PASSWORD_RESET_UNAVAILABLE");
        assertThat(failed.getResponse().getCookie(REFRESH).getMaxAge()).isZero();
        jdbc.execute("DROP TRIGGER test_fail_user_update");
        assertThat(jdbc.queryForObject("SELECT auth_version FROM users WHERE email = ?", Long.class, email)).isZero();
        login(email, PASSWORD);
        assertStatus(call(HttpMethod.GET, "/api/users/me", null, original.access()), 200);
        assertStatus(reset(email, proof, "Replacement-pass1!"), 409);
    }

    @Test
    void passwordChangeInvalidatesOutstandingResetProofAndCode() throws Exception {
        String email = register();
        Login original = login(email, PASSWORD);
        String proof = resetProof(email);
        String change = accountProof(original, email, "PASSWORD_CHANGE");
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", change, "newPassword", "Changed-password1!"), original.access()), 204);
        assertStatus(reset(email, proof, "unexpected-password"), 400);
        login(email, "Changed-password1!");
        redisReset("DEL", "cooldown", email);
        MvcResult issued = requestReset(email);
        Map<String, String> confirmation = resetConfirmation(email, issued);
        jdbc.update("UPDATE users SET auth_version = auth_version + 1 WHERE email = ?", email);
        assertStatus(call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", confirmation, null), 409);
    }

    @Test
    void fifthPasswordFailureReturnsTheRequired429ContractWithoutExtendingTheBlock() throws Exception {
        String email = email();
        for (int i = 0; i < 4; i++) {
            MvcResult failed = loginAttempt(email, "wrong-password", "192.0.2.85", true);
            assertStatus(failed, 401);
            assertThat(json(failed).path("remainingAttempts").asInt()).isEqualTo(4 - i);
        }
        MvcResult fifth = loginAttempt(" " + email.toUpperCase() + " ", "wrong-password", "192.0.2.85", true);
        assertStatus(fifth, 429);
        assertThat(json(fifth).path("code").asText()).isEqualTo("LOGIN_RATE_LIMITED");
        assertThat(json(fifth).path("retryAfterSeconds").asLong()).isEqualTo(300);
        assertThat(json(fifth).has("remainingAttempts")).isFalse();
        assertThat(fifth.getResponse().getHeader("Retry-After")).isEqualTo("300");
        assertThat(fifth.getResponse().getHeader("Cache-Control")).contains("no-store");
        assertThat(fifth.getResponse().getCookie(REFRESH)).isNull();
        String key = "sayroam:auth:{login-attempts}:email:" + VerificationDigests.sha256(email);
        Object deadline = redis.opsForHash().get(key, "blockedUntil");
        assertStatus(loginAttempt(email, "wrong-password", "198.51.100.85", true), 429);
        assertThat(redis.opsForHash().get(key, "blockedUntil")).isEqualTo(deadline);
        assertThat(redis.opsForHash().get(key, "failures")).isEqualTo("5");
    }

    @Test
    void correctFifthAttemptResetsEmailAndTheNextFailureStillHasFourAttempts() throws Exception {
        String email = register();
        for (int i = 0; i < 4; i++) assertStatus(loginAttempt(email, "wrong-password", "192.0.2.85", true), 401);
        assertStatus(loginAttempt(email, PASSWORD, "192.0.2.85", true), 200);
        assertThat(redis.hasKey("sayroam:auth:{login-attempts}:email:" + VerificationDigests.sha256(email))).isFalse();
        MvcResult nextFailure = loginAttempt(email, "wrong-password", "192.0.2.85", true);
        assertStatus(nextFailure, 401);
        assertThat(json(nextFailure).path("remainingAttempts").asInt()).isEqualTo(4);
        MvcResult otherEmailFailure = loginAttempt(email(), "wrong-password", "192.0.2.85", true);
        assertStatus(otherEmailFailure, 401);
        assertThat(json(otherEmailFailure).path("remainingAttempts").asInt()).isEqualTo(4);
        assertStatus(loginAttempt(email, PASSWORD, "192.0.2.85", true), 200);
        assertThat(redis.opsForHash().get("sayroam:auth:{login-attempts}:ip-wide:" + VerificationDigests.sha256("192.0.2.85"), "failures")).isEqualTo("6");
    }

    @Test
    void hundredthIpFailureBlocksAcrossEmailsAndForgedForwardedHeadersCannotBypassIt() throws Exception {
        String email = register();
        String ipKey = "sayroam:auth:{login-attempts}:ip-wide:" + VerificationDigests.sha256("192.0.2.85");
        redis.opsForHash().put(ipKey, "failures", "99");
        redis.expire(ipKey, Duration.ofSeconds(300));
        MvcResult blocked = loginAttempt(email(), "wrong-password", "192.0.2.85", true);
        assertStatus(blocked, 429);
        assertThat(json(blocked).path("retryAfterSeconds").asInt()).isEqualTo(300);
        assertThat(json(blocked).has("remainingAttempts")).isFalse();
        assertThat(blocked.getResponse().getHeader("Retry-After")).isEqualTo("300");
        assertThat(blocked.getResponse().getCookie(REFRESH)).isNull();
        // X-Forwarded-For를 바꿔도 실제 remoteAddr 기준의 IP 제한은 유지된다.
        assertStatus(loginAttempt(email, PASSWORD, "192.0.2.85", true), 429);
        assertStatus(loginAttempt(email, PASSWORD, "198.51.100.85", true), 200);
        assertThat(redis.opsForHash().get(ipKey, "failures")).isEqualTo("100");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rememberChoiceSurvivesRefreshRotation(boolean rememberMe) throws Exception {
        String email = register();
        MvcResult signedIn = loginAttempt(email, PASSWORD, "192.0.2.85", rememberMe);
        assertStatus(signedIn, 200);
        Cookie cookie = signedIn.getResponse().getCookie(REFRESH);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge() > 0).isEqualTo(rememberMe);
        assertThat(cookie.isHttpOnly()).isTrue();
        MvcResult refreshed = call(HttpMethod.POST, "/api/auth/refresh", null, null, cookie);
        assertStatus(refreshed, 200);
        assertThat(refreshed.getResponse().getCookie(REFRESH).getMaxAge() > 0).isEqualTo(rememberMe);
    }

    @ParameterizedTest
    @ValueSource(strings = {"long", "spaces"})
    void legacyPasswordsCanStillLoginAndChangeToTheNewPolicy(String kind) throws Exception {
        String email = register();
        String legacy = kind.equals("long") ? "x".repeat(128) : "  old password  ";
        jdbc.update("UPDATE user_credential SET password_hash = ? WHERE user_id = (SELECT id FROM users WHERE email = ?)", passwords.encode(legacy), email);
        Login oldLogin = login(email, legacy);
        String proof = accountProof(oldLogin, email, "PASSWORD_CHANGE");
        assertStatus(call(HttpMethod.PATCH, "/api/auth/password", Map.of("accountProof", proof, "newPassword", "UpdatedPass1!"), oldLogin.access()), 204);
        login(email, "UpdatedPass1!");
    }

    @Test
    void registeredEmailsAreRejectedBeforeAnySignupEmailIsSent() throws Exception {
        String email = register();
        mail.codes.remove(email + ":signup");
        String key = "sayroam:auth:{email-verification}:state:" + VerificationDigests.sha256(email);
        Map<Object, Object> previous = redis.opsForHash().entries(key);
        MvcResult denied = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", " " + email.toUpperCase() + " "), null);
        assertStatus(denied, 409);
        assertThat(json(denied).path("code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(mail.code(email, "signup")).isNull();
        assertThat(redis.opsForHash().entries(key)).isEqualTo(previous);
        jdbc.update("DELETE FROM user_credential WHERE user_id = (SELECT id FROM users WHERE email = ?)", email);
        assertStatus(call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email), null), 409);
    }

    @Test
    void databaseUniquenessStillProtectsSignupAfterTheEmailAvailabilityCheck() throws Exception {
        String email = email();
        String proof = signupProof(email);
        registrations.register(email, "동시 가입", passwords.encode(PASSWORD), Instant.now());
        MvcResult conflict = call(HttpMethod.POST, "/api/auth/signup", Map.of("email", email, "nickname", "후속 가입", "password", PASSWORD, "signupProof", proof), null);
        assertStatus(conflict, 409);
        assertThat(json(conflict).path("code").asText()).isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Long.class, email)).isEqualTo(1L);
    }

    @Test
    void unavailableLoginLimitStorageReturns503WithoutIssuingCookies() throws Exception {
        DockerClient docker = org.testcontainers.DockerClientFactory.instance().client();
        docker.pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            MvcResult failed = loginAttempt(email(), "wrong-password", "192.0.2.85", true);
            assertStatus(failed, 503);
            assertThat(json(failed).path("code").asText()).isEqualTo("LOGIN_ATTEMPTS_UNAVAILABLE");
            assertThat(failed.getResponse().getCookie(REFRESH)).isNull();
            assertThat(failed.getResponse().getHeader("Retry-After")).isNull();
        } finally {
            docker.unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    @Test
    void loginOpenApiRequiresRetryTimingAndDocumentsPersistence() throws Exception {
        JsonNode spec = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());
        JsonNode response = spec.path("components").path("responses").path("LoginRateLimited");
        assertThat(response.path("headers").path("Retry-After").path("required").asBoolean()).isTrue();
        assertThat(response.path("content").path("application/problem+json").path("schema").path("$ref").asText()).endsWith("/LoginRateLimitedProblem");
        assertThat(spec.path("components").path("schemas").path("LoginRateLimitedProblem").toString()).contains("retryAfterSeconds", "LOGIN_RATE_LIMITED", "minimum");
        assertThat(spec.path("components").path("schemas").path("LoginUnauthorizedProblem").toString()).contains("remainingAttempts", "INVALID_CREDENTIALS", "required");
        assertThat(spec.path("components").path("schemas").path("LoginRequest").path("properties").has("rememberMe")).isTrue();
        assertThat(spec.path("components").path("schemas").path("LoginRequest").path("required").toString()).doesNotContain("rememberMe");
    }

    private MvcResult loginAttempt(String email, String password, String ip, boolean rememberMe) throws Exception {
        String path = "/api/auth/login";
        return mvc.perform(post(path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                        .header("X-Forwarded-For", UUID.randomUUID().toString())
                        .with(request -> { request.setRemoteAddr(ip); return request; })
                        .contentType("application/json").content(mapper.writeValueAsString(Map.of("email", email, "password", password, "rememberMe", rememberMe))))
                .andReturn();
    }

    @Test
    void selectsSignupAndResetEmailLanguageForEachBrowserRequest() throws Exception {
        for (String path : List.of("/api/auth/email-verifications", "/api/auth/password-reset-verifications")) {
            String purpose = path.contains("password-reset") ? "PASSWORD_RESET" : "signup";
            for (String header : List.of("en-US,en;q=0.9,ko;q=0.8", "ko-KR,ko;q=0.9,en;q=0.8", "ja-JP")) {
                String email = email();
                mvc.perform(post(path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                                .header("Accept-Language", header).contentType("application/json")
                                .content(mapper.writeValueAsString(Map.of("email", email))))
                        .andExpect(status().isAccepted());
                assertThat(mail.languages.get(email + ":" + purpose)).isEqualTo(header.startsWith("en") ? EmailLanguage.EN : EmailLanguage.KO);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"en-GB,en;q=0.9", "ko-KR,ko;q=0.9"})
    void passesBrowserLanguageToBothAccountVerificationPurposes(String header) throws Exception {
        for (AccountVerificationPurpose purpose : AccountVerificationPurpose.values()) {
            String email = register();
            Login login = login(email, PASSWORD);
            String path = "/api/auth/account-verifications";
            mvc.perform(post(path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue())
                            .header("Authorization", "Bearer " + login.access()).header("Accept-Language", header)
                            .contentType("application/json").content(mapper.writeValueAsString(Map.of("purpose", purpose.name()))))
                    .andExpect(status().isAccepted());
            assertThat(mail.languages.get(email + ":" + purpose.name())).isEqualTo(header.startsWith("en") ? EmailLanguage.EN : EmailLanguage.KO);
        }
    }

    @Test
    void declaresOptionalBrowserLanguageOnAllEmailIssuanceOperations() throws Exception {
        MvcResult result = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode spec = json(result);
        for (String path : List.of("/api/auth/email-verifications", "/api/auth/password-reset-verifications", "/api/auth/account-verifications")) {
            JsonNode parameters = spec.path("paths").path(path).path("post").path("parameters");
            assertThat(parameters.isArray()).isTrue();
            JsonNode language = java.util.stream.StreamSupport.stream(parameters.spliterator(), false)
                    .filter(parameter -> "Accept-Language".equals(parameter.path("name").asText())).findFirst().orElseThrow();
            assertThat(language.path("in").asText()).isEqualTo("header");
            assertThat(language.path("required").asBoolean()).isFalse();
        }
    }

    @Test
    void resetOpenApiDeclaresPublicCsrfOnlyOperationsAndDedicatedProof() throws Exception {
        MvcResult document = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode spec = json(document);
        for (Map.Entry<String, String> route : Map.of("/api/auth/password-reset-verifications", "requestPasswordReset",
                "/api/auth/password-reset-verifications/verify", "confirmPasswordReset",
                "/api/auth/password-reset", "resetPassword").entrySet()) {
            JsonNode operation = spec.path("paths").path(route.getKey()).path("post");
            assertThat(operation.path("operationId").asText()).isEqualTo(route.getValue());
            assertThat(operation.path("security").size()).isEqualTo(1);
            assertThat(operation.path("security").get(0).has("csrfToken")).isTrue();
            assertThat(operation.path("security").get(0).size()).isEqualTo(1);
            assertThat(operation.path("responses").has("403")).isTrue();
        }
        JsonNode required = spec.path("components").path("schemas").path("PasswordResetRequest").path("required");
        assertThat(required.toString()).contains("email", "resetProof", "newPassword");
    }

    private MvcResult requestReset(String email) throws Exception {
        return call(HttpMethod.POST, "/api/auth/password-reset-verifications", Map.of("email", email), null);
    }

    private Map<String, String> resetConfirmation(String email, MvcResult issued) {
        return Map.of("email", email, "verificationId", json(issued).path("verificationId").asText(),
                "code", mail.code(email.toLowerCase(java.util.Locale.ROOT), "PASSWORD_RESET"));
    }

    private String resetProof(String email) throws Exception {
        MvcResult issued = requestReset(email);
        assertStatus(issued, 202);
        MvcResult confirmed = call(HttpMethod.POST, "/api/auth/password-reset-verifications/verify", resetConfirmation(email, issued), null);
        assertStatus(confirmed, 200);
        return json(confirmed).path("resetProof").asText();
    }

    private MvcResult reset(String email, String proof, String password) throws Exception {
        return call(HttpMethod.POST, "/api/auth/password-reset", Map.of("email", email, "resetProof", proof, "newPassword", password), null);
    }

    private String redisReset(String command, String type, String email, String... values) throws Exception {
        ArrayList<String> args = new java.util.ArrayList<String>(java.util.List.of("redis-cli", command,
                "sayroam:auth:{password-reset}:" + type + ":" + VerificationDigests.sha256(email)));
        args.addAll(java.util.List.of(values));
        Container.ExecResult result = REDIS.execInContainer(args.toArray(String[]::new));
        assertThat(result.getExitCode()).isZero();
        return result.getStdout().strip();
    }

    private String register() throws Exception {
        String email = email();
        signup(email, signupProof(email));
        return email;
    }

    private String signupProof(String email) throws Exception {
        MvcResult issued = call(HttpMethod.POST, "/api/auth/email-verifications", Map.of("email", email), null);
        assertStatus(issued, 202);
        MvcResult confirmed = call(HttpMethod.POST, "/api/auth/email-verifications/verify",
                Map.of("email", email, "verificationId", json(issued).path("verificationId").asText(), "code", mail.code(email, "signup")), null);
        assertStatus(confirmed, 200);
        return json(confirmed).path("signupProof").asText();
    }

    private long signup(String email, String proof) throws Exception {
        MvcResult result = call(HttpMethod.POST, "/api/auth/signup", Map.of("email", email, "password", PASSWORD, "nickname", "여행자", "signupProof", proof), null);
        assertStatus(result, 201);
        return json(result).path("userId").asLong();
    }

    private Login login(String email, String password) throws Exception {
        MvcResult result = call(HttpMethod.POST, "/api/auth/login", Map.of("email", email, "password", password), null);
        assertStatus(result, 200);
        assertThat(json(result).has("refreshToken")).isFalse();
        assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
        return new Login(json(result).path("accessToken").asText(), result.getResponse().getCookie(REFRESH));
    }

    private String accountProof(Login login, String email, String purpose) throws Exception {
        MvcResult issued = call(HttpMethod.POST, "/api/auth/account-verifications", Map.of("purpose", purpose), login.access());
        assertStatus(issued, 202);
        MvcResult confirmed = call(HttpMethod.POST, "/api/auth/account-verifications/verify", Map.of("purpose", purpose,
                "verificationId", json(issued).path("verificationId").asText(), "code", mail.code(email, purpose)), login.access());
        assertStatus(confirmed, 200);
        return json(confirmed).path("accountProof").asText();
    }

    private MvcResult call(HttpMethod method, String path, Object body, String access, Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder builder = request(method, path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue());
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
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            java.util.concurrent.Callable<MvcResult> gated = () -> { ready.countDown(); start.await(10, TimeUnit.SECONDS); return action.call(); };
            Future<MvcResult> one = pool.submit(gated);
            Future<MvcResult> two = pool.submit(gated);
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
        final Map<String, EmailLanguage> languages = new ConcurrentHashMap<>();
        volatile boolean fail;
        private void capture(String recipient, String purpose, String code, EmailLanguage language) {
            assertThat(recipient).endsWith("@example.test");
            if (fail) throw new AuthException(AuthErrorCode.EMAIL_DELIVERY_FAILED);
            codes.put(recipient + ":" + purpose, code);
            languages.put(recipient + ":" + purpose, language);
        }
        String code(String recipient, String purpose) { return codes.get(recipient + ":" + purpose); }
        @Override public void sendVerificationCode(String recipient, String code, EmailLanguage language) { capture(recipient, "signup", code, language); }
        @Override public void sendAccountVerificationCode(String recipient, String code, AccountVerificationPurpose purpose, EmailLanguage language) { capture(recipient, purpose.name(), code, language); }
        @Override public void sendPasswordResetCode(String recipient, String code, EmailLanguage language) { capture(recipient, "PASSWORD_RESET", code, language); }
    }
}
