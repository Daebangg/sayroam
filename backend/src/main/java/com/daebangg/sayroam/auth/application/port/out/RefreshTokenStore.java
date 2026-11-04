package com.daebangg.sayroam.auth.application.port.out;

public interface RefreshTokenStore {
    /** 새 로그인을 저장하고 남은 절대 유효기간(초)을 반환한다. */
    long create(String sessionId, long userId, String tokenDigest);

    /** 현재 토큰을 확인한다. 이미 사용한 토큰이면 해당 로그인의 갱신 권한을 폐기한다. */
    SessionResult inspect(String sessionId, String tokenDigest);

    /** 현재 토큰 비교·교체·사용 이력 기록을 원자적으로 수행한다. */
    SessionResult rotate(String sessionId, String currentDigest, String nextDigest);

    /** 현재 또는 이미 사용한 토큰의 소유가 확인될 때만 해당 로그인을 폐기한다. 없거나 이미 폐기된 상태는 성공으로 처리한다. */
    void revoke(String sessionId, String tokenDigest);

    enum Status { ACTIVE, INVALID, REUSED }

    record SessionResult(Status status, long userId, long expiresIn) {
    }
}
