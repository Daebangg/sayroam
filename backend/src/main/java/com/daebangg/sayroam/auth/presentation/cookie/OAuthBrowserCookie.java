package com.daebangg.sayroam.auth.presentation.cookie;

import com.daebangg.sayroam.auth.application.error.AuthErrorCode;
import com.daebangg.sayroam.auth.application.error.AuthException;
import com.daebangg.sayroam.auth.domain.OAuthSecrets;
import com.daebangg.sayroam.auth.presentation.config.OAuthBrowserCookieProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class OAuthBrowserCookie {
    private final OAuthBrowserCookieProperties properties;

    public OAuthBrowserCookie(OAuthBrowserCookieProperties properties) { this.properties = properties; }

    public String prepare(HttpServletRequest request) {
        String value = readOptional(request);
        return value == null ? OAuthSecrets.create() : value;
    }

    public String require(HttpServletRequest request) {
        String value = readOptional(request);
        if (value == null) throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
        return value;
    }

    public ResponseCookie issue(String value) {
        return ResponseCookie.from(name(), value).httpOnly(true).secure(properties.secure())
                .sameSite("Lax").path("/").maxAge(properties.maxAgeSeconds()).build();
    }

    private String readOptional(HttpServletRequest request) {
        String value = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name().equals(cookie.getName())) {
                    if (value != null || !OAuthSecrets.isValid(cookie.getValue())) {
                        throw new AuthException(AuthErrorCode.SOCIAL_AUTH_INVALID);
                    }
                    value = cookie.getValue();
                }
            }
        }
        return value;
    }

    private String name() { return properties.secure() ? "__Host-sayroam_oauth" : "sayroam_oauth"; }
}
