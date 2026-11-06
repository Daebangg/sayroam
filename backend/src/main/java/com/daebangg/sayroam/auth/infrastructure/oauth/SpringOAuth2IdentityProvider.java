package com.daebangg.sayroam.auth.infrastructure.oauth;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.auth.application.port.out.SocialIdentityProvider;
import com.daebangg.sayroam.user.domain.SocialProvider;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.client.endpoint.RestClientAuthorizationCodeTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationExchange;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationResponse;
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Locale;
import java.util.Map;

/** 모든 공급자의 코드 교환과 UserInfo 조회를 Spring OAuth2 Client로 처리하는 단일 어댑터. */
@Component
public class SpringOAuth2IdentityProvider implements SocialIdentityProvider {
    private final ClientRegistrationRepository clients;
    private final OAuth2UserInfoMapper mapper;
    private final RestClientAuthorizationCodeTokenResponseClient tokens;
    private final DefaultOAuth2UserService users;

    public SpringOAuth2IdentityProvider(ClientRegistrationRepository clients, OAuth2UserInfoMapper mapper) {
        this.clients = clients;
        this.mapper = mapper;
        var requests = new SimpleClientHttpRequestFactory();
        requests.setConnectTimeout(3000);
        requests.setReadTimeout(5000);
        var rest = RestClient.builder().requestFactory(requests)
                .configureMessageConverters(converters -> converters.addCustomConverter(new OAuth2AccessTokenResponseHttpMessageConverter()))
                .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler()).build();
        this.tokens = new RestClientAuthorizationCodeTokenResponseClient();
        this.tokens.setRestClient(rest);
        this.tokens.addParametersConverter(grant -> {
            var parameters = new LinkedMultiValueMap<String, String>();
            if (Boolean.TRUE.equals(grant.getClientRegistration().getProviderDetails().getConfigurationMetadata().get("token-request-state"))) {
                parameters.add("state", grant.getAuthorizationExchange().getAuthorizationRequest().getState());
            }
            return parameters;
        });
        var userInfoRest = new RestTemplate(requests);
        userInfoRest.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        this.users = new DefaultOAuth2UserService();
        this.users.setRestOperations(userInfoRest);
        this.users.setAttributesConverter(request -> attributes -> {
            String idAttribute = request.getClientRegistration().getProviderDetails()
                    .getUserInfoEndpoint().getUserNameAttributeName();
            if (attributes.get(idAttribute) == null) {
                throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
            }
            return attributes;
        });
    }

    @Override
    public URI authorizationUri(SocialProvider provider, AuthorizationRequest request) {
        var client = client(provider);
        var authorization = request(client, request.state());
        if (client.getClientSettings().isRequireProofKey()) {
            authorization.additionalParameters(Map.of(PkceParameterNames.CODE_CHALLENGE, request.codeChallenge(),
                    PkceParameterNames.CODE_CHALLENGE_METHOD, "S256"));
        }
        return URI.create(authorization.build().getAuthorizationRequestUri());
    }

    @Override
    public SocialIdentity authenticate(SocialProvider provider, Callback callback) {
        var client = client(provider);
        try {
            var authorization = request(client, callback.state());
            if (client.getClientSettings().isRequireProofKey()) {
                authorization.attributes(attributes -> attributes.put(PkceParameterNames.CODE_VERIFIER, callback.codeVerifier()));
            }
            var response = OAuth2AuthorizationResponse.success(callback.authorizationCode()).state(callback.state())
                    .redirectUri(client.getRedirectUri()).build();
            var grant = new OAuth2AuthorizationCodeGrantRequest(client, new OAuth2AuthorizationExchange(authorization.build(), response));
            var token = tokens.getTokenResponse(grant);
            var user = users.loadUser(new OAuth2UserRequest(client, token.getAccessToken(), token.getAdditionalParameters()));
            return mapper.map(provider, user.getAttributes());
        } catch (OAuth2AuthorizationException | OAuth2AuthenticationException exception) {
            // 공급자 응답·토큰을 담을 수 있는 메시지를 외부 응답이나 로그로 전달하지 않는다.
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        } catch (RestClientException exception) {
            throw new AuthException(AuthErrorCode.SOCIAL_AUTH_UNAVAILABLE);
        }
    }

    private ClientRegistration client(SocialProvider provider) {
        var client = clients.findByRegistrationId(provider.name().toLowerCase(Locale.ROOT));
        if (client == null) throw new AuthException(AuthErrorCode.SOCIAL_PROVIDER_DISABLED);
        return client;
    }

    private OAuth2AuthorizationRequest.Builder request(ClientRegistration client, String state) {
        return OAuth2AuthorizationRequest.authorizationCode().authorizationUri(client.getProviderDetails().getAuthorizationUri())
                .clientId(client.getClientId()).redirectUri(client.getRedirectUri()).scopes(client.getScopes()).state(state);
    }
}
