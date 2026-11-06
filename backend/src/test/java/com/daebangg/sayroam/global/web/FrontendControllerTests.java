package com.daebangg.sayroam.global.web;

import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.controller.AuthCsrfController;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({FrontendController.class, AuthCsrfController.class})
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
class FrontendControllerTests {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @ParameterizedTest
    @ValueSource(strings = {"/trips", "/trips/", "/matching", "/rooms", "/brand", "/brand/",
            "/TRIPS", "/Trips/", "/MATCHING", "/ROOMS/", "/BRAND", "/Brand/"})
    void 화면_직접_접속은_인증_없이_앱_셸을_제공한다(String path) throws Exception {
        mvc.perform(get(path).accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/unknown-page", "/unknown-page/nested", "/trips/123", "/matching/123", "/rooms/123/chat"})
    void 없는_화면은_404_상태와_앱_셸을_제공한다(String path) throws Exception {
        mvc.perform(get(path).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(forwardedUrl("/index.html"))
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }

    @Test
    void 루트는_Spring_Boot의_기본_welcome_page를_사용한다() throws Exception {
        mvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void 정적_자원은_원래_형식으로_제공한다() throws Exception {
        mvc.perform(get("/assets/test.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("margin: 0")));
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Sayroam test shell")));
        mvc.perform(MockMvcRequestBuilders.head("/trips").servletPath("/trips"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mvc.perform(MockMvcRequestBuilders.head("/assets/test.css").servletPath("/assets/test.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
    void 공개_파일_경로여도_GET_HEAD_외에는_인증이_필요하다(String method) throws Exception {
        mvc.perform(MockMvcRequestBuilders.request(HttpMethod.valueOf(method), "/assets/test.css")
                        .servletPath("/assets/test.css"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 보호_API의_HEAD_요청은_공개하지_않는다() throws Exception {
        mvc.perform(MockMvcRequestBuilders.head("/api/users/me").servletPath("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/assets/missing.js", "/assets/missing", "/brand/missing.svg", "/brand/missing",
            "/fonts/missing.woff2", "/images/missing.png", "/missing.js", "/trips/missing.js", "/rooms/nested/missing.css"})
    void 없는_정적_자원은_HTML_대신_404를_응답한다(String path) throws Exception {
        mvc.perform(get(path).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/users/me", "/api/missing", "/api/auth/missing", "/error", "/actuator/health", "/webjars/missing.js", "/login", "/logout", "/oauth2/authorization/google"})
    void 서버_경로는_화면_공개_정책에_포함하지_않는다(String path) throws Exception {
        mvc.perform(get(path).accept(MediaType.TEXT_HTML))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void 인증된_없는_API는_404를_유지한다() throws Exception {
        mvc.perform(get("/api/missing").with(user("123").roles("USER")).accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 관리자_권한과_CSRF_검사를_유지한다() throws Exception {
        mvc.perform(get("/api/admin/users").with(user("123").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login").servletPath("/api/auth/login"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("INVALID_CSRF_TOKEN"));
        mvc.perform(get("/api/auth/csrf"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void 화면_경로의_JSON_요청과_변경_요청을_HTML로_바꾸지_않는다() throws Exception {
        mvc.perform(get("/trips").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotAcceptable());
        mvc.perform(MockMvcRequestBuilders.post("/trips").with(user("123").roles("USER")))
                .andExpect(status().isMethodNotAllowed());
    }

    private static MockHttpServletRequestBuilder get(String path) {
        return MockMvcRequestBuilders.get(path).with(request -> {
            request.setServletPath(path);
            return request;
        });
    }
}
