package com.daebangg.sayroam.auth;

import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.auth.application.port.out.VerificationEmailSender;
import com.daebangg.sayroam.auth.application.service.AccountWriteService;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.auth.infrastructure.identity.OAuth2UserInfoMapper;
import com.daebangg.sayroam.auth.infrastructure.persistence.AccountLookupJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.user.domain.SocialProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 실제 보안 필터·Redis·MySQL을 사용하고 외부 공급자와 메일 발송은 대체한다. */
@AutoConfigureMockMvc
class SocialLinkIntegrationTests extends DatabaseIntegrationTest {
    private static final String EMAIL = "member@example.test";
    private static final String PASSWORD = "Test-password-123";
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired AccountWriteService accountWrites;
    @Autowired OAuth2UserInfoMapper userInfos;
    @MockitoBean SocialIdentityProvider identities;
    @MockitoBean VerificationEmailSender mailSender;
    @MockitoSpyBean AccountLookupJpaRepository accountQueries;
    private Cookie csrf;

    @BeforeEach void prepare() throws Exception {
        clean();
        csrf = mvc.perform(get("/api/auth/csrf").servletPath("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
        when(identities.availableProviders()).thenReturn(List.of(SocialProvider.GOOGLE));
        when(identities.authorizationUri(any(SocialProvider.class), any())).thenAnswer(call ->
                URI.create("https://accounts.google.com/o/oauth2/v2/auth?state=" + call.getArgument(1, SocialIdentityProvider.AuthorizationRequest.class).state()));
        when(identities.authenticate(eq(SocialProvider.GOOGLE), any())).thenReturn(identity("subject", EMAIL, true));
    }

    @AfterEach void clean() throws Exception {
        for (String table : List.of("expense_share", "expense", "trip_place", "trip", "companion_request", "companion_search",
                "room_invitation", "room_member", "chat_message", "room", "place", "user_travel_style", "travel_style_item",
                "notification", "user_social_account", "user_credential", "users")) jdbc.update("DELETE FROM " + table);
        assertThat(REDIS.execInContainer("redis-cli", "FLUSHDB").getExitCode()).isZero();
    }

    @Test void verifiedMatchingEmailConnectsAndSignsInWithoutPasswordOrAnotherRequest() throws Exception {
        long id = register();
        String hash = jdbc.queryForObject("select password_hash from user_credential where user_id = ?", String.class, id);
        Returned result = returned();
        assertThat(countLinks()).isZero();
        MvcResult response = exchange(result);
        assertAuthenticatedAs(response, id);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("newlyRegistered").asBoolean()).isFalse();
        assertThat(response.getResponse().getCookie("sayroam_refresh")).isNotNull();
        assertThat(countLinks()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select nickname from users where id = ?", String.class, id)).isEqualTo("기존 여행자");
        assertThat(jdbc.queryForObject("select password_hash from user_credential where user_id = ?", String.class, id)).isEqualTo(hash);
        assertAuthenticatedAs(exchange(returned()), id);
    }

    @ParameterizedTest @NullSource @ValueSource(strings = {EMAIL})
    void missingOrUnverifiedEmailDoesNotConnectToAnExistingMember(String email) throws Exception {
        register();
        when(identities.authenticate(eq(SocialProvider.GOOGLE), any())).thenReturn(identity("subject", email, false));
        MvcResult response = exchange(returned());
        assertThat(response.getResponse().getStatus()).isEqualTo(400);
        JsonNode body = json.readTree(response.getResponse().getContentAsByteArray());
        assertThat(body.path("code").asText()).isEqualTo("SOCIAL_EMAIL_REQUIRED");
        assertThat(response.getResponse().getCookie("sayroam_refresh")).isNull();
        assertThat(countLinks()).isZero();
    }

    @ParameterizedTest @ValueSource(strings = {"auth_version = auth_version + 1", "deleted_at = UTC_TIMESTAMP()", "email_verified_at = NULL", "email = 'changed@example.test'"})
    void accountChangesBetweenCallbackAndExchangeCannotAuthorizeTheOldIdentity(String change) throws Exception {
        long id = register();
        Returned result = returned();
        jdbc.update("update users set " + change + " where id = ?", id);
        MvcResult response = exchange(result);
        assertThat(response.getResponse().getStatus()).isIn(400, 401);
        assertThat(response.getResponse().getCookie("sayroam_refresh")).isNull();
        assertThat(countLinks()).isZero();
    }

    @Test void unverifiedExistingMemberCannotBeAutomaticallyLinked() throws Exception {
        long id = register();
        jdbc.update("update users set email_verified_at = NULL where id = ?", id);
        MvcResult response = exchange(returned());
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("EMAIL_ALREADY_REGISTERED");
        assertThat(countLinks()).isZero();
    }

    @Test void aDifferentIdentityForAnAlreadyConnectedProviderIsNotReplaced() throws Exception {
        accountWrites.registerSocial(identity("original-subject", EMAIL, true), EMAIL, "기존 여행자", Instant.now());
        MvcResult response = exchange(returned());
        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("SOCIAL_ACCOUNT_CONFLICT");
        assertThat(jdbc.queryForObject("select provider_user_id from user_social_account", String.class)).isEqualTo("original-subject");
        assertThat(countLinks()).isEqualTo(1);
    }

    @Test
    void emailVerificationReportsAuthenticationUnavailableWhenEmailLookupFails() throws Exception {
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(accountQueries).existsByEmail(EMAIL);

        MvcResult result = postJson("/api/auth/email-verifications", Map.of("email", EMAIL));

        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        assertThat(json.readTree(result.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("AUTHENTICATION_UNAVAILABLE");
        verifyNoInteractions(mailSender);
    }

    @Test
    void socialExchangeReportsSocialAuthUnavailableWhenSharedEmailLookupFails() throws Exception {
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(accountQueries).existsByEmail(EMAIL);

        MvcResult result = exchange(returned());

        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        assertThat(json.readTree(result.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("SOCIAL_AUTH_UNAVAILABLE");
        assertThat(result.getResponse().getCookie("sayroam_refresh")).isNull();
    }

    @Test
    void socialExchangeReportsSocialAuthUnavailableWhenProviderLookupFails() throws Exception {
        doThrow(new DataAccessResourceFailureException("database unavailable"))
                .when(accountQueries).findActiveSocialAccount(SocialProvider.GOOGLE, "subject");

        MvcResult result = exchange(returned());

        assertThat(result.getResponse().getStatus()).isEqualTo(503);
        assertThat(json.readTree(result.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("SOCIAL_AUTH_UNAVAILABLE");
        assertThat(result.getResponse().getCookie("sayroam_refresh")).isNull();
    }

    @Test
    void duplicateProviderIdentityRollsBackTheNewMember() {
        String subject = "shared-subject";
        String anotherEmail = "another@example.test";
        accountWrites.registerSocial(identity(subject, EMAIL, true), EMAIL, "기존 여행자", Instant.now());

        assertThatThrownBy(() -> accountWrites.registerSocial(
                identity(subject, anotherEmail, true), anotherEmail, "신규 여행자", Instant.now()))
                .isInstanceOfSatisfying(AuthException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.SOCIAL_ACCOUNT_CONFLICT));

        assertThat(countLinks()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from users where email = ?", Long.class, anotherEmail)).isZero();
    }

    @Test
    void databaseFailureWhileLoadingExistingSocialAccountsReturns503() throws Exception {
        long userId = register();
        Returned pending = returned();
        jdbc.update("""
                INSERT INTO user_social_account (user_id, provider, provider_user_id, created_at, updated_at)
                VALUES (?, 'KAKAO', 'other-provider-subject', UTC_TIMESTAMP(), UTC_TIMESTAMP())
                """, userId);

        MvcResult response = SocialAccountReadFailure.during(jdbc, () -> exchange(pending));

        assertThat(response.getResponse().getStatus()).isEqualTo(503);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("SOCIAL_AUTH_UNAVAILABLE");
        assertThat(response.getResponse().getCookie("sayroam_refresh")).isNull();
        assertThat(countLinks()).isEqualTo(1);
    }

    @Test
    void identityClaimedAfterCallbackCannotBeLinkedToAnotherMember() throws Exception {
        long originalMemberId = register();
        Returned pending = returned();
        String anotherEmail = "another@example.test";
        long linkedMemberId = accountWrites.registerSocial(
                identity("subject", anotherEmail, true), anotherEmail, "다른 여행자", Instant.now()).userId();

        MvcResult response = exchange(pending);

        assertThat(response.getResponse().getStatus()).isEqualTo(409);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("code").asText())
                .isEqualTo("SOCIAL_ACCOUNT_CONFLICT");
        assertThat(response.getResponse().getCookie("sayroam_refresh")).isNull();
        assertThat(countLinks()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select user_id from user_social_account where provider_user_id = ?",
                Long.class, "subject")).isEqualTo(linkedMemberId).isNotEqualTo(originalMemberId);
    }

    @Test void concurrentVerifiedLoginsLinkOnceAndAuthenticateAsTheSameMember() throws Exception {
        long id = register();
        Returned first = returned(); Returned second = returned();
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch gate = new CountDownLatch(1);
            Future<MvcResult> one = executor.submit(() -> { gate.await(); return exchange(first); });
            Future<MvcResult> two = executor.submit(() -> { gate.await(); return exchange(second); });
            gate.countDown();
            assertAuthenticatedAs(one.get(10, TimeUnit.SECONDS), id);
            assertAuthenticatedAs(two.get(10, TimeUnit.SECONDS), id);
        }
        assertThat(countLinks()).isEqualTo(1);
    }

    @Test void wrongBrowserDoesNotConsumeTheResultAndAConsumedResultCannotLoginAgain() throws Exception {
        long id = register(); Returned result = returned();
        assertThat(exchange(new Returned(result.code(), new Cookie(result.browser().getName(), OAuthSecrets.create()))).getResponse().getStatus()).isEqualTo(400);
        assertThat(countLinks()).isZero();
        assertAuthenticatedAs(exchange(result), id);
        assertThat(exchange(result).getResponse().getStatus()).isEqualTo(400);
    }

    @Test void schemaKeepsTheAuthenticatedResponseWithoutLinkingSteps() throws Exception {
        byte[] response = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        JsonNode spec = json.readTree(response);
        assertThat(spec.path("paths").has("/api/auth/social/links")).isFalse();
        assertThat(spec.path("components").path("schemas").path("SocialExchangeResponse").path("properties").has("link")).isFalse();
        assertThat(spec.path("components").path("schemas").path("SocialSignupRequest").path("properties").has("signupProof")).isFalse();
        Path output = java.nio.file.Path.of("build/openapi/openapi.json");
        java.nio.file.Files.createDirectories(output.getParent()); java.nio.file.Files.write(output, response);
    }

    @ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"NAVER", "KAKAO"})
    void providerEmailSignsIntoTheExistingMemberWithoutSendingVerificationMail(SocialProvider provider) throws Exception {
        long id = register();
        when(identities.authenticate(eq(provider), any())).thenReturn(userInfos.map(provider, profile(provider, EMAIL)));
        assertAuthenticatedAs(exchange(returned(provider)), id);
    }

    @ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"NAVER", "KAKAO"})
    void newProviderMemberSignsUpWithProviderNicknameAndRelogsInWithoutEmailConsent(SocialProvider provider) throws Exception {
        when(identities.authenticate(eq(provider), any())).thenReturn(userInfos.map(provider, profile(provider, EMAIL)));
        assertAuthenticatedAsNewProvider(exchange(returned(provider)), provider);
        when(identities.authenticate(eq(provider), any())).thenReturn(userInfos.map(provider, profile(provider, null)));
        assertAuthenticatedAs(exchange(returned(provider)), jdbc.queryForObject("select id from users where email = ?", Long.class, EMAIL));
    }

    @ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"NAVER", "KAKAO"})
    void missingProviderEmailRequestsConsentWithoutIssuingASignupTicket(SocialProvider provider) throws Exception {
        when(identities.authenticate(eq(provider), any())).thenReturn(userInfos.map(provider, profile(provider, null)));
        MvcResult response = exchange(returned(provider));
        assertThat(response.getResponse().getStatus()).isEqualTo(400);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("code").asText()).isEqualTo("SOCIAL_EMAIL_REQUIRED");
        assertThat(jdbc.queryForObject("select count(*) from users", Long.class)).isZero();
    }

    private Map<String, Object> profile(SocialProvider provider, String email) {
        if (provider == SocialProvider.NAVER) return Map.of("resultcode", "00", "response", email == null ? Map.of("id", "subject") : Map.of("id", "subject", "email", email, "nickname", "네이버 별명"));
        return Map.of("id", "subject", "kakao_account", email == null ? Map.of() : Map.of("email", email, "is_email_valid", true, "is_email_verified", true, "profile", Map.of("nickname", "카카오 별명")));
    }

    @ParameterizedTest @NullAndEmptySource @ValueSource(strings = {"   ", "\u200B"})
    void missingProviderNameUsesDefaultNicknameWithoutASetupScreen(String name) throws Exception {
        when(identities.authenticate(eq(SocialProvider.NAVER), any())).thenReturn(new SocialIdentity(SocialProvider.NAVER, "subject", EMAIL, true, name, null));
        MvcResult response = exchange(returned(SocialProvider.NAVER));
        assertThat(response.getResponse().getStatus()).isEqualTo(200);
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("status").asText()).isEqualTo("AUTHENTICATED");
        assertThat(jdbc.queryForObject("select nickname from users where email = ?", String.class, EMAIL)).isEqualTo("여행자");
    }

    @Test void longProviderNameRespectsTheExistingNicknameLimit() throws Exception {
        when(identities.authenticate(eq(SocialProvider.NAVER), any())).thenReturn(userInfos.map(SocialProvider.NAVER,
                Map.of("resultcode", "00", "response", Map.of("id", "subject", "email", EMAIL, "nickname", "가".repeat(70)))));
        assertThat(exchange(returned(SocialProvider.NAVER)).getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select nickname from users where email = ?", String.class, EMAIL)).isEqualTo("가".repeat(50));
    }

    @Test void naverProvidedNameIsUsedWhenNicknameIsNotShared() throws Exception {
        when(identities.authenticate(eq(SocialProvider.NAVER), any())).thenReturn(userInfos.map(SocialProvider.NAVER,
                Map.of("resultcode", "00", "response", Map.of("id", "subject", "email", EMAIL, "name", "네이버 이름"))));
        assertThat(exchange(returned(SocialProvider.NAVER)).getResponse().getStatus()).isEqualTo(200);
        assertThat(jdbc.queryForObject("select nickname from users where email = ?", String.class, EMAIL)).isEqualTo("네이버 이름");
    }

    private void assertAuthenticatedAsNewProvider(MvcResult response, SocialProvider provider) throws Exception {
        assertThat(json.readTree(response.getResponse().getContentAsByteArray()).path("newlyRegistered").asBoolean()).isTrue();
        long id = jdbc.queryForObject("select id from users where email = ?", Long.class, EMAIL);
        assertAuthenticatedAs(response, id);
        assertThat(jdbc.queryForObject("select nickname from users where id = ?", String.class, id)).isEqualTo(provider == SocialProvider.NAVER ? "네이버 별명" : "카카오 별명");
        assertThat(jdbc.queryForObject("select count(*) from user_credential", Long.class)).isZero();
    }

    private long register() { return accountWrites.register(EMAIL, "기존 여행자", passwords.encode(PASSWORD), Instant.now()); }
    private long countLinks() { return jdbc.queryForObject("select count(*) from user_social_account", Long.class); }
    private SocialIdentity identity(String subject, String email, boolean verified) { return new SocialIdentity(SocialProvider.GOOGLE, subject, email, verified, "소셜 이름", null); }
    private void assertAuthenticatedAs(MvcResult result, long id) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        JsonNode body = json.readTree(result.getResponse().getContentAsByteArray());
        assertThat(body.path("status").asText()).isEqualTo("AUTHENTICATED");
        assertThat(body.path("signup").isNull()).isTrue();
        MvcResult me = mvc.perform(get("/api/users/me").servletPath("/api/users/me").header("Authorization", "Bearer " + body.path("login").path("accessToken").asText())).andExpect(status().isOk()).andReturn();
        assertThat(json.readTree(me.getResponse().getContentAsByteArray()).path("userId").asLong()).isEqualTo(id);
    }
    private Returned returned() throws Exception {
        return returned(SocialProvider.GOOGLE);
    }
    private Returned returned(SocialProvider provider) throws Exception {
        String name = provider.name().toLowerCase(java.util.Locale.ROOT);
        MvcResult start = postJson("/api/auth/social/" + name + "/authorizations", Map.of("returnPath", "/"));
        assertThat(start.getResponse().getStatus()).isEqualTo(200);
        Cookie browser = Arrays.stream(start.getResponse().getCookies()).filter(cookie -> cookie.getName().endsWith("oauth")).findFirst().orElseThrow();
        String address = json.readTree(start.getResponse().getContentAsByteArray()).path("authorizationUrl").asText();
        String state = URI.create(address).getRawQuery().substring("state=".length());
        String location = mvc.perform(get("/api/auth/social/" + name + "/callback").servletPath("/api/auth/social/" + name + "/callback").param("state", state).param("code", "provider-code").cookie(browser))
                .andExpect(status().isFound()).andReturn().getResponse().getHeader("Location");
        return new Returned(URI.create(location).getFragment().substring("social_result=".length()), browser);
    }
    private MvcResult exchange(Returned result) throws Exception { return postJson("/api/auth/social/results/exchange", Map.of("resultCode", result.code()), result.browser()); }
    private MvcResult postJson(String path, Object body, Cookie... cookies) throws Exception {
        MockHttpServletRequestBuilder builder = post(path).servletPath(path).cookie(csrf).header("X-XSRF-TOKEN", csrf.getValue()).contentType("application/json").content(json.writeValueAsString(body));
        if (cookies.length > 0) builder.cookie(cookies);
        return mvc.perform(builder).andReturn();
    }
    private record Returned(String code, Cookie browser) {}
}
