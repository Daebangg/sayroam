package com.daebangg.sayroam.global.web;

import com.daebangg.sayroam.auth.presentation.config.RefreshCookieProperties;
import com.daebangg.sayroam.auth.presentation.cookie.RefreshTokenCookie;
import com.daebangg.sayroam.global.security.ActiveAccountJwtConverter;
import com.daebangg.sayroam.global.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FrontendController.class)
@Import({SecurityConfig.class, RefreshTokenCookie.class})
@EnableConfigurationProperties(RefreshCookieProperties.class)
@ActiveProfiles("local")
class LocalFrontendResourcesTests {

    @TempDir
    static Path frontendDirectory;

    @BeforeAll
    static void frontendIndex() throws Exception {
        Files.writeString(frontendDirectory.resolve("index.html"), "<!doctype html><html>local shell</html>");
    }

    @DynamicPropertySource
    static void frontendLocation(DynamicPropertyRegistry registry) {
        registry.add("spring.web.resources.static-locations", () -> frontendDirectory.toUri().toString());
    }

    @Autowired
    MockMvc mvc;

    @MockitoBean
    JwtDecoder accessTokenDecoder;

    @MockitoBean
    ActiveAccountJwtConverter accountConverter;

    @Test
    void 루트와_화면_주소는_정적_index로_forward한다() throws Exception {
        mvc.perform(get("/").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
        mvc.perform(get("/rooms/123/chat").accept(MediaType.TEXT_HTML))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void 로컬_index는_기본_정적_핸들러가_HTML로_제공한다() throws Exception {
        mvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string("<!doctype html><html>local shell</html>"));
    }

    @Test
    void 로컬_빌드에서_삭제된_자원은_재시작_없이_404를_응답한다() throws Exception {
        Path asset = Files.createDirectories(frontendDirectory.resolve("assets")).resolve("old.js");
        Files.writeString(asset, "export const version = 1;");
        mvc.perform(get("/assets/old.js"))
                .andExpect(status().isOk())
                .andExpect(content().string("export const version = 1;"));

        Files.delete(asset);

        mvc.perform(get("/assets/old.js"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
