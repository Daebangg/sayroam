package com.daebangg.sayroam.auth.application.service;

import com.daebangg.sayroam.auth.application.port.out.RefreshTokenStore;
import com.daebangg.sayroam.auth.domain.RefreshToken;
import org.springframework.stereotype.Service;

@Service
public class LogoutService {
    private final RefreshTokenStore sessions;

    public LogoutService(RefreshTokenStore sessions) {
        this.sessions = sessions;
    }

    public void logout(String rawToken) {
        // 쿠키가 없거나 형식이 잘못돼도 브라우저 정리는 허용한다.
        // 정상 형식이면 현재/이전 토큰의 소유 확인과 폐기를 저장소가 원자적으로 수행한다.
        RefreshToken.parse(rawToken).ifPresent(token -> sessions.revoke(token.sessionId(), token.digest()));
    }
}
