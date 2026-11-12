package com.daebangg.sayroam.auth.domain;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
class SocialAuthSettingsTests {
 private SocialAuthSettings policy(URI origin) { return new SocialAuthSettings(Duration.ofSeconds(300),Duration.ofSeconds(60),Duration.ofSeconds(600),Set.of("/"),origin); }
 @Test void defaultSameOriginAndExplicitHttpsOrLoopbackOriginAreAllowed() {
  assertThat(policy(null).frontendOrigin()).isNull();
  assertThat(policy(URI.create("http://localhost:5173")).frontendOrigin().getPort()).isEqualTo(5173);
  assertThat(policy(URI.create("https://app.example.test")).frontendOrigin().getHost()).isEqualTo("app.example.test");
 }
 @Test void pathsCredentialsQueriesFragmentsAndUnencryptedExternalOriginsAreRejected() {
  for (String value:List.of("https://app.example.test/path","https://user@app.example.test","https://app.example.test?next=/","https://app.example.test#fragment","http://app.example.test","//app.example.test"))
   assertThatThrownBy(()->policy(URI.create(value))).as(value).isInstanceOf(IllegalArgumentException.class);
 }
}
