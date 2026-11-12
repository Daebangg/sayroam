package com.daebangg.sayroam.auth.infrastructure.identity;

import com.daebangg.sayroam.auth.application.dto.SocialIdentity;
import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import com.daebangg.sayroam.user.domain.SocialProvider;
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
                Map<?, ?> account = object(attributes, "kakao_account");
                Map<?, ?> profile = object(account, "profile");
                boolean verified = Boolean.TRUE.equals(account.get("is_email_valid"))
                        && Boolean.TRUE.equals(account.get("is_email_verified"));
                yield identity(provider, text(attributes, "id"), text(account, "email"), verified,
                        text(profile, "nickname"), text(profile, "profile_image_url"));
            }
            case NAVER -> {
                if (!"00".equals(text(attributes, "resultcode"))) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
                Map<?, ?> response = object(attributes, "response");
                String nickname = text(response, "nickname");
                // 인증된 네이버 UserInfo가 제공한 이메일을 서비스 이메일로 수락한다. 별도 인증 메일은 보내지 않는다.
                yield identity(provider, text(response, "id"), text(response, "email"), true,
                        nickname == null || nickname.isBlank() ? text(response, "name") : nickname, text(response, "profile_image"));
            }
        };
    }

    private SocialIdentity identity(SocialProvider provider, String id, String email, boolean verified, String name, String picture) {
        if (id == null || id.isBlank() || id.length() > 100) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_FAILED);
        if (email != null && (!com.daebangg.sayroam.auth.domain.EmailAddress.isValid(email))) email = null;
        return new SocialIdentity(provider, id, email, verified && email != null, bounded(name, 500), bounded(picture, 500));
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
