package com.daebangg.sayroam.auth.infrastructure.oauth;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.application.model.SocialIdentity;
import com.daebangg.sayroam.user.domain.type.SocialProvider;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;

/** 공급자 JSON의 ID·이메일·프로필 필드 차이만 정규화한다. */
@Component
public class OAuth2UserInfoMapper {
    public SocialIdentity map(SocialProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> {
                String email = text(attributes, "email");
                String domain = text(attributes, "hd");
                boolean verified = Boolean.TRUE.equals(attributes.get("verified_email")) && email != null
                        && (email.toLowerCase(Locale.ROOT).endsWith("@gmail.com") || (domain != null && !domain.isBlank()));
                yield identity(provider, text(attributes, "id"), email, verified,
                        text(attributes, "name"), text(attributes, "picture"));
            }
            case KAKAO -> {
                var account = object(attributes, "kakao_account");
                var profile = object(account, "profile");
                boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                        && Boolean.TRUE.equals(account.get("is_email_verified"));
                yield identity(provider, text(attributes, "id"), text(account, "email"), verified,
                        text(profile, "nickname"), text(profile, "profile_image_url"));
            }
            case NAVER -> {
                if (!"00".equals(text(attributes, "resultcode"))) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
                var response = object(attributes, "response");
                // 이메일 검증 여부를 제공하지 않으므로 서비스의 이메일 보완 인증을 사용한다.
                yield identity(provider, text(response, "id"), text(response, "email"), false,
                        text(response, "nickname"), text(response, "profile_image"));
            }
        };
    }

    private SocialIdentity identity(SocialProvider provider, String id, String email, boolean verified, String name, String picture) {
        if (id == null || id.isBlank() || id.length() > 100) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        if (email != null && (email.length() > 254 || !email.matches("[!-~]+@[!-~]+"))) email = null;
        return new SocialIdentity(provider, id, email, verified && email != null, bounded(name, 50), bounded(picture, 500));
    }

    private String text(Map<?, ?> values, String key) {
        Object value = values.get(key);
        return value instanceof String string ? string : value instanceof Number number ? number.toString() : null;
    }
    private Map<?, ?> object(Map<?, ?> values, String key) {
        return values.get(key) instanceof Map<?, ?> map ? map : Map.of();
    }
    private String bounded(String value, int limit) { return value != null && value.length() <= limit ? value : null; }
}
