package com.daebangg.sayroam.auth.presentation.cookie;

import com.daebangg.sayroam.auth.domain.error.AuthErrorCode;
import com.daebangg.sayroam.auth.domain.error.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookie {
    public static final String NAME = "sayroam_refresh";
    private final RefreshCookieProperties properties;

    public RefreshTokenCookie(RefreshCookieProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie issue(String value, long expiresIn) {
        return issue(value, expiresIn, true);
    }

    public ResponseCookie issue(String value, long expiresIn, boolean rememberMe) {
        if (expiresIn < 1) {
            throw new IllegalArgumentException("Refresh cookie requires a positive lifetime");
        }
        return cookie(value, rememberMe ? expiresIn : -1);
    }

    public ResponseCookie clear() {
        return cookie("", 0);
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String value = null;
        boolean found = false;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (NAME.equals(cookie.getName())) {
                    // 경로가 다른 중복 쿠키 중 임의의 하나를 선택하지 않는다.
                    if (found) {
                        throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
                    }
                    found = true;
                    value = cookie.getValue();
                }
            }
        }
        return value;
    }

    private ResponseCookie cookie(String value, long maxAge) {
        return ResponseCookie.from(NAME, value).httpOnly(true).secure(properties.secure())
                .sameSite("Strict").path("/api/auth").maxAge(maxAge).build();
    }
}
