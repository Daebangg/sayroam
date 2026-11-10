package com.daebangg.sayroam.auth;

import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import java.net.URI;
import java.util.Arrays;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SocialFrontendIntegrationTests extends DatabaseIntegrationTest {
 @Autowired MockMvc mvc;
 @Autowired JsonMapper mapper;
 @DynamicPropertySource static void frontend(DynamicPropertyRegistry registry) {
  registry.add("app.auth.social.google.enabled",()->true);
  registry.add("app.auth.social.google.client-id",()->"test-google-client");
  registry.add("app.auth.social.google.client-secret",()->"test-only-not-a-secret");
  registry.add("app.auth.social.google.redirect-uri",()->"http://localhost:8080/api/auth/social/google/callback");
  registry.add("app.auth.social.kakao.enabled",()->false);
  registry.add("app.auth.social.naver.enabled",()->false);
  registry.add("app.auth.social.frontend-origin",()->"http://localhost:5173");
 }
 @Test void publicProvidersExposeOnlyEnabledNamesWithoutSecretsOrLoginCookies() throws Exception {
  MvcResult result=mvc.perform(get("/api/auth/social/providers").servletPath("/api/auth/social/providers")).andExpect(status().isOk()).andReturn();
  JsonNode body=mapper.readTree(result.getResponse().getContentAsByteArray());
  assertThat(body.size()).isEqualTo(1);
  assertThat(body.path("providers").toString()).isEqualTo("[\"GOOGLE\"]");
  assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store");
  assertThat(Arrays.stream(result.getResponse().getCookies()).map(cookie->cookie.getName()).toList()).allMatch(name->name.equals("XSRF-TOKEN"));
 }
 @Test void cancelledProviderReturnsOnlyAOneTimeCodeToTheConfiguredFrontend() throws Exception {
  Cookie csrf=mvc.perform(get("/api/auth/csrf").servletPath("/api/auth/csrf")).andReturn().getResponse().getCookie("XSRF-TOKEN");
  MvcResult start=mvc.perform(post("/api/auth/social/google/authorizations").servletPath("/api/auth/social/google/authorizations")
   .cookie(csrf).header("X-XSRF-TOKEN",csrf.getValue()).contentType("application/json").content("{\"returnPath\":\"/\"}")).andExpect(status().isOk()).andReturn();
  Cookie browser=Arrays.stream(start.getResponse().getCookies()).filter(cookie->cookie.getName().endsWith("oauth")).findFirst().orElseThrow();
  URI address=URI.create(mapper.readTree(start.getResponse().getContentAsByteArray()).path("authorizationUrl").asText());
  String state=Arrays.stream(address.getRawQuery().split("&")).filter(part->part.startsWith("state=")).findFirst().orElseThrow().substring(6);
  MvcResult result=mvc.perform(get("/api/auth/social/google/callback").servletPath("/api/auth/social/google/callback")
   .param("state",state).param("error","access_denied").cookie(browser)).andExpect(status().isFound()).andReturn();
  assertThat(result.getResponse().getHeader("Location")).matches("http://localhost:5173/#social_result=[A-Za-z0-9_-]{43}");
  assertThat(result.getResponse().getHeader("Referrer-Policy")).isEqualTo("no-referrer");
 }
}
