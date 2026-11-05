package com.daebangg.sayroam.auth.domain;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/** 로그인 식별자와 256비트 난수. 원문은 클라이언트에만 전달하고 저장에는 digest를 사용한다. */
public final class RefreshToken {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern FORMAT = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\\.[A-Za-z0-9_-]{43}");
    private final String value;

    private RefreshToken(String value) {
        this.value = value;
    }

    public static RefreshToken create() {
        return generate(UUID.randomUUID().toString());
    }

    public static Optional<RefreshToken> parse(String value) {
        return value != null && value.length() == 80 && FORMAT.matcher(value).matches()
                ? Optional.of(new RefreshToken(value)) : Optional.empty();
    }

    public RefreshToken next() {
        return generate(sessionId());
    }

    private static RefreshToken generate(String sessionId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return new RefreshToken(sessionId + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes));
    }

    public String sessionId() {
        return value.substring(0, 36);
    }

    public String digest() {
        return VerificationDigests.sha256(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "RefreshToken[redacted]";
    }
}
