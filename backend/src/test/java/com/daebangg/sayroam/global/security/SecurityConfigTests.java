package com.daebangg.sayroam.global.security;

import com.daebangg.sayroam.auth.presentation.controller.AuthCsrfController;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({SecurityConfigTests.TestEndpoints.class, AuthCsrfController.class})
@Import({SecurityConfig.class, SecurityConfigTests.TestEndpoints.class})
class SecurityConfigTests {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/signup", "/api/auth/login"})
    void publicPostsDoNotRequireAuthentication(String path) throws Exception {
        mvc.perform(post(path).with(csrfCookieAndHeader()))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users/me", "/api/admin/users", "/api/auth/login", "/login", "/error"})
    void anonymousRequestsToProtectedPathsReturn401WithoutRedirectOrSession(String path) throws Exception {
        mvc.perform(get(path))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }

    @Test
    void authNamespaceIsNotEntirelyPublic() throws Exception {
        mvc.perform(post("/api/auth/account-link").with(csrfCookieAndHeader()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCanAccessMemberEndpointWithoutCreatingSession() throws Exception {
        mvc.perform(get("/api/users/me").with(user("123").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void memberCannotAccessAdminEndpoint() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("123").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAdminEndpoint() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("1").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void existingSessionAuthenticationIsNotUsed() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var authentication = UsernamePasswordAuthenticationToken.authenticated(
                "123", null, AuthorityUtils.createAuthorityList("ROLE_USER"));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(authentication));

        mvc.perform(get("/api/users/me").session(session))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void basicCredentialsDoNotAuthenticateRequests() throws Exception {
        mvc.perform(get("/api/users/me").with(httpBasic("user", "password")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("WWW-Authenticate"));
    }

    @Test
    void defaultLogoutEndpointIsDisabled() throws Exception {
        mvc.perform(post("/logout").with(csrfCookieAndHeader()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void publicPostWithoutCsrfTokenIsRejectedWithoutCreatingSession() throws Exception {
        mvc.perform(post("/api/auth/login"))
                .andExpect(status().isForbidden())
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull());
    }

    @Test
    void invalidCsrfTokenIsRejected() throws Exception {
        Cookie csrfCookie = csrfCookie();
        mvc.perform(post("/api/auth/signup")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", csrfCookie.getValue() + "invalid"))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalErrorDispatchIsNotOverwrittenByAuthenticationFailure() throws Exception {
        mvc.perform(get("/api/test-error").with(request -> {
                    request.setDispatcherType(DispatcherType.ERROR);
                    return request;
                }))
                .andExpect(status().isOk());
    }

    private Cookie csrfCookie() throws Exception {
        return mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andExpect(cookie().doesNotExist("JSESSIONID"))
                .andExpect(result -> assertThat(result.getRequest().getSession(false)).isNull())
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
    }

    private RequestPostProcessor csrfCookieAndHeader() throws Exception {
        Cookie csrfCookie = csrfCookie();
        // csrf() replaces the shared filter's repository; use the browser's cookie/header flow instead.
        return request -> {
            request.setCookies(csrfCookie);
            request.addHeader("X-XSRF-TOKEN", csrfCookie.getValue());
            return request;
        };
    }

    // 접근 정책만 검증하는 테스트 전용 API. 실제 가입·로그인 구현은 제공하지 않는다.
    @RestController
    static class TestEndpoints {

        @PostMapping({"/api/auth/signup", "/api/auth/login", "/api/auth/account-link"})
        String authEndpoint() {
            return "ok";
        }

        @GetMapping({"/api/users/me", "/api/admin/users", "/api/test-error"})
        String protectedEndpoint() {
            return "ok";
        }
    }
}
