package com.daebangg.sayroam.trip.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

// 여행마다 하나만 두는 공유 링크다. 이력을 남기지 않아 만료되면 고치지 않고 새 값으로 바꾼다
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripInvitation {
    private static final int VALID_DAYS = 7;
    private static final int CODE_RANDOM_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Getter
    @Column(name = "invitation_code", length = 50, unique = true)
    private String code;

    @Column(name = "invitation_expires_at")
    private LocalDateTime expiresAt;

    private TripInvitation(String code, LocalDateTime expiresAt) {
        this.code = code;
        this.expiresAt = expiresAt;
    }

    static TripInvitation issue(LocalDateTime issuedAt) {
        return new TripInvitation(generateCode(), issuedAt.plusDays(VALID_DAYS));
    }

    // 링크에 넣는 값이라 추측할 수 없는 난수를 URL에 안전한 문자로 만든다
    private static String generateCode() {
        byte[] bytes = new byte[CODE_RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }
}
