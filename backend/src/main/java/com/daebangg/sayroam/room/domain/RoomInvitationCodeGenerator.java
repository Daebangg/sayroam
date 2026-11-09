package com.daebangg.sayroam.room.domain;

import java.security.SecureRandom;
import java.util.Base64;

// 링크에 넣는 값이라 추측할 수 없는 난수를 URL에 안전한 문자로 만든다
final class RoomInvitationCodeGenerator {

    private static final int RANDOM_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RoomInvitationCodeGenerator() {
    }

    static String generate() {
        byte[] bytes = new byte[RANDOM_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
