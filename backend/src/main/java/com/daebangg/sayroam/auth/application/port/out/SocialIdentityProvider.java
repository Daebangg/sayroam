package com.daebangg.sayroam.auth.application.port.out;

import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.user.domain.SocialProvider;

import java.net.URI;

/** 외부 OAuth2 인증 포트. Spring 객체와 공급자 응답은 어댑터 안에 둔다. */
public interface SocialIdentityProvider {
    java.util.List<SocialProvider> availableProviders();

    /** 서버의 공급자 설정과 state로 인증 주소를 만든다. 지원하는 공급자에는 PKCE S256을 적용한다. */
    URI authorizationUri(SocialProvider provider, AuthorizationRequest request);

    /**
     * 임시 상태의 공급자·브라우저 바인딩을 확인하고 소비한 뒤 호출한다.
     * 코드를 공급자 토큰으로 교환하고 UserInfo API로 확인한 사용자 정보만 반환한다.
     * Redirect URI는 어댑터의 신뢰한 서버 설정을 사용한다.
     */
    SocialIdentity authenticate(SocialProvider provider, Callback request);

    record AuthorizationRequest(String state, String codeChallenge, boolean requestEmailConsent) {
        @Override
        public String toString() {
            return "AuthorizationRequest[redacted]";
        }
    }

    record Callback(String authorizationCode, String state, String codeVerifier) {
        @Override
        public String toString() {
            return "Callback[redacted]";
        }
    }
}
