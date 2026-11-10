package com.daebangg.sayroam.auth.infrastructure.oauth;

import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.net.URI;
import static org.assertj.core.api.Assertions.assertThat;

class SocialEmailConsentTests {
    @Test void naverConsentRetryRepromptsWithoutAskingForCredentialsAgain() {
        ClientRegistration client = ClientRegistration.withRegistrationId("naver").clientId("test-client").clientSecret("test-only")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/api/auth/social/naver/callback")
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize")
                .tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me").userNameAttributeName("id")
                .clientSettings(ClientRegistration.ClientSettings.builder().requireProofKey(false).build()).build();
        SpringOAuth2IdentityProvider adapter = new SpringOAuth2IdentityProvider(id -> client, new OAuth2UserInfoMapper());
        URI normal = adapter.authorizationUri(SocialProvider.NAVER, new SocialIdentityProvider.AuthorizationRequest("test-state", "challenge", false));
        URI retry = adapter.authorizationUri(SocialProvider.NAVER, new SocialIdentityProvider.AuthorizationRequest("test-state", "challenge", true));
        assertThat(normal.getQuery()).contains("auth_type=reauthenticate").doesNotContain("prompt=select_account");
        assertThat(retry.getQuery()).contains("auth_type=reprompt", "state=test-state").doesNotContain("reauthenticate", "code_challenge");
    }

    @ParameterizedTest @EnumSource(value = SocialProvider.class, names = {"GOOGLE", "KAKAO"})
    void accountSelectionIsRequestedWithoutLosingStateOrPkce(SocialProvider provider) {
        String name = provider.name().toLowerCase(java.util.Locale.ROOT);
        String endpoint = provider == SocialProvider.GOOGLE ? "https://accounts.google.com/o/oauth2/v2/auth" : "https://kauth.kakao.com/oauth/authorize";
        ClientRegistration client = ClientRegistration.withRegistrationId(name).clientId("test-client").clientSecret("test-only")
                .scope("account_email", "profile_nickname")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/api/auth/social/" + name + "/callback")
                .authorizationUri(endpoint).tokenUri("https://provider.example.test/token")
                .userInfoUri("https://provider.example.test/me").userNameAttributeName("id")
                .clientSettings(ClientRegistration.ClientSettings.builder().requireProofKey(true).build()).build();
        SpringOAuth2IdentityProvider adapter = new SpringOAuth2IdentityProvider(id -> client, new OAuth2UserInfoMapper());
        URI address = adapter.authorizationUri(provider, new SocialIdentityProvider.AuthorizationRequest("test-state", "test-challenge", false));
        assertThat(address.getQuery()).contains("prompt=select_account", "state=test-state", "code_challenge=test-challenge", "code_challenge_method=S256");
        assertThat(address.getQuery()).doesNotContain("auth_type", "client_secret");
        if (provider == SocialProvider.KAKAO) {
            assertThat(address.getQuery()).contains("scope=account_email,profile_nickname");
            assertThat(java.util.Arrays.stream(address.getQuery().split("&")).filter(part -> part.startsWith("scope=")).count()).isEqualTo(1);
        }
    }
}
