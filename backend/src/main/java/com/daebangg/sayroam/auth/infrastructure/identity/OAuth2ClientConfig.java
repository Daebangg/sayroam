package com.daebangg.sayroam.auth.infrastructure.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** 공급자별 차이를 Authorization Code와 UserInfo 클라이언트 설정으로 표현한다. */
@Configuration(proxyBeanMethods = false)
public class OAuth2ClientConfig {
    @Bean
    ClientRegistrationRepository socialClientRegistrations(OAuth2ClientProperties properties) {
        HashMap<String, ClientRegistration> registrations = new HashMap<String, ClientRegistration>();
        register(registrations, properties.google(), ClientRegistration.withRegistrationId("google")
                .clientName("Google").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .scope("https://www.googleapis.com/auth/userinfo.email", "https://www.googleapis.com/auth/userinfo.profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://www.googleapis.com/oauth2/v2/userinfo").userNameAttributeName("id"), "google", true);
        register(registrations, properties.kakao(), ClientRegistration.withRegistrationId("kakao")
                .clientName("Kakao").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .scope("account_email", "profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize").tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me?secure_resource=true").userNameAttributeName("id")
                .providerConfigurationMetadata(Map.of("user-info-content-type", "application/x-www-form-urlencoded;charset=utf-8")),
                "kakao", true);
        register(registrations, properties.naver(), ClientRegistration.withRegistrationId("naver")
                .clientName("Naver").clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationUri("https://nid.naver.com/oauth2.0/authorize").tokenUri("https://nid.naver.com/oauth2.0/token")
                .userInfoUri("https://openapi.naver.com/v1/nid/me").userNameAttributeName("id")
                .providerConfigurationMetadata(Map.of("token-request-state", true,
                        "user-info-id-container", "response")), "naver", false);
        Map<String, ClientRegistration> clients = Map.copyOf(registrations);
        return clients::get;
    }

    private void register(Map<String, ClientRegistration> registrations, OAuth2ClientProperties.Client settings,
                          ClientRegistration.Builder builder, String provider, boolean pkce) {
        if (!settings.enabled()) return;
        URI redirect = settings.redirectUri();
        if (redirect == null || redirect.getHost() == null || redirect.getUserInfo() != null
                || redirect.getRawQuery() != null || redirect.getRawFragment() != null
                || !("/api/auth/social/" + provider + "/callback").equals(redirect.getRawPath())
                || !("https".equalsIgnoreCase(redirect.getScheme()) || ("http".equalsIgnoreCase(redirect.getScheme())
                && Set.of("localhost", "127.0.0.1", "[::1]").contains(redirect.getHost().toLowerCase(Locale.ROOT))))) {
            throw new IllegalStateException("OAuth2 redirect-uri requires an HTTPS provider callback (HTTP loopback only)");
        }
        registrations.put(provider, builder.clientId(settings.clientId()).clientSecret(settings.clientSecret())
                .redirectUri(redirect.toASCIIString()).authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientSettings(ClientRegistration.ClientSettings.builder().requireProofKey(pkce).build()).build());
    }
}
