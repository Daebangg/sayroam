package com.daebangg.sayroam.auth.infrastructure.identity;

import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.SocialProvider;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequestEntityConverter;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** 모든 공급자의 코드 교환과 UserInfo 조회를 Spring OAuth2 Client로 처리하는 단일 어댑터. */
@Component
public class SpringOAuth2IdentityProvider implements SocialIdentityProvider {
    private final ClientRegistrationRepository clients;
    private final OAuth2UserInfoMapper mapper;
    private final RestClientAuthorizationCodeTokenResponseClient tokens;
    private final DefaultOAuth2UserService users;

    @Override
    public java.util.List<SocialProvider> availableProviders() {
        return java.util.Arrays.stream(SocialProvider.values())
                .filter(provider -> clients.findByRegistrationId(provider.name().toLowerCase(Locale.ROOT)) != null)
                .toList();
    }

    public SpringOAuth2IdentityProvider(ClientRegistrationRepository clients, OAuth2UserInfoMapper mapper) {
        this.clients = clients;
        this.mapper = mapper;
        SimpleClientHttpRequestFactory requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(3000);
        requests.setReadTimeout(5000);
        RestClient rest = RestClient.builder().requestFactory(requests)
                .configureMessageConverters(converters -> converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
        this.tokens = new RestClientAuthorizationCodeTokenResponseClient();
        this.tokens.setRestClient(rest);
        this.tokens.addParametersConverter(grant -> {
            LinkedMultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
            if (Boolean.TRUE.equals(grant.getClientRegistration().getProviderDetails().getConfigurationMetadata().get("token-request-state"))) {
                parameters.add("state", grant.getAuthorizationExchange().getAuthorizationRequest().getState());
            }
            return parameters;
        });
        RestTemplate userInfoRest = new RestTemplate(requests);
        userInfoRest.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.users = new DefaultOAuth2UserService();
        this.users.setRestOperations(userInfoRest);
        OAuth2UserRequestEntityConverter userInfoRequests = new OAuth2UserRequestEntityConverter();
        this.users.setRequestEntityConverter(request -> {
            RequestEntity<?> entity = userInfoRequests.convert(request);
            Object contentType = request.getClientRegistration().getProviderDetails().getConfigurationMetadata().get("user-info-content-type");
            if (contentType instanceof String value) {
                // 공급자의 추가 헤더 요구만 적용하고 Spring이 만든 Bearer 인증·요청 본문은 유지한다.
                return RequestEntity.method(entity.getMethod(), entity.getUrl()).headers(entity.getHeaders())
                        .contentType(MediaType.parseMediaType(value)).body(entity.getBody());
            }
            return entity;
        });
        this.users.setAttributesConverter(request -> attributes -> {
            String idAttribute = request.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            Object container = request.getClientRegistration().getProviderDetails().getConfigurationMetadata().get("user-info-id-container");
            Map<?, ?> idAttributes = attributes;
            if (container instanceof String key) {
                if (!(attributes.get(key) instanceof Map<?, ?> nested)) {
                    throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
                }
                idAttributes = nested;
            }
            Object id = idAttributes.get(idAttribute);
            if (id == null) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            if (idAttributes == attributes) return attributes;
            // Spring의 사용자 식별자는 최상위에 두고, 공급자 결과 코드와 중첩 응답은 매퍼를 위해 보존한다.
            HashMap<String, Object> normalized = new HashMap<>(attributes);
            normalized.put(idAttribute, id);
            return normalized;
        });
    }

    @Override
    public URI authorizationUri(SocialProvider provider, AuthorizationRequest request) {
        ClientRegistration client = client(provider);
        OAuth2AuthorizationRequest.Builder authorization = request(client, request.state());
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        if (client.getClientSettings().isRequireProofKey()) {
            parameters.put(PkceParameterNames.CODE_CHALLENGE, request.codeChallenge());
            parameters.put(PkceParameterNames.CODE_CHALLENGE_METHOD, "S256");
        }
        if (provider == SocialProvider.NAVER) {
            parameters.put("auth_type", request.requestEmailConsent() ? "reprompt" : "reauthenticate");
        } else {
            parameters.put("prompt", "select_account");
            if (provider == SocialProvider.KAKAO && !client.getScopes().isEmpty()) {
                parameters.put("scope", String.join(",", client.getScopes().stream().sorted().toList()));
            }
        }
        authorization.additionalParameters(parameters);
        return URI.create(authorization.build().getAuthorizationRequestUri());
    }

    @Override
    public SocialIdentity authenticate(SocialProvider provider, Callback callback) {
        ClientRegistration client = client(provider);
        try {
            OAuth2AuthorizationRequest.Builder authorization = request(client, callback.state());
            if (client.getClientSettings().isRequireProofKey()) {
                authorization.attributes(attributes -> attributes.put(PkceParameterNames.CODE_VERIFIER, callback.codeVerifier()));
            }
            OAuth2AuthorizationResponse response = OAuth2AuthorizationResponse.success(callback.authorizationCode()).state(callback.state())
                    .redirectUri(client.getRedirectUri()).build();
            OAuth2AuthorizationCodeGrantRequest grant = new OAuth2AuthorizationCodeGrantRequest(client, new OAuth2AuthorizationExchange(authorization.build(), response));
            OAuth2AccessTokenResponse token = tokens.getTokenResponse(grant);
            OAuth2User user = users.loadUser(new OAuth2UserRequest(client, token.getAccessToken(), token.getAdditionalParameters()));
            return mapper.map(provider, user.getAttributes());
        } catch (OAuth2AuthorizationException | OAuth2AuthenticationException exception) {
            // 공급자 응답·토큰을 담을 수 있는 메시지를 외부 응답이나 로그로 전달하지 않는다.
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        } catch (RestClientException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
    }

    private ClientRegistration client(SocialProvider provider) {
        ClientRegistration client = clients.findByRegistrationId(provider.name().toLowerCase(Locale.ROOT));
        if (client == null) throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_DISABLED);
        return client;
    }

    private OAuth2AuthorizationRequest.Builder request(ClientRegistration client, String state) {
        return OAuth2AuthorizationRequest.authorizationCode().authorizationUri(client.getProviderDetails().getAuthorizationUri())
                .clientId(client.getClientId()).redirectUri(client.getRedirectUri()).scopes(client.getScopes()).state(state);
    }
}
