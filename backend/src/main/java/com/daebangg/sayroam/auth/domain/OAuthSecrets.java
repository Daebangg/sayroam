package com.daebangg.sayroam.auth.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** state·브라우저 바인딩·결과 코드·PKCE에 사용하는 256비트 난수. */
public final class OAuthSecrets {
    private static final SecureRandom RANDOM = new SecureRandom();

    private OAuthSecrets() { }

    public static String create() {
        byte[] value = new byte[32];
        RANDOM.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    public static boolean isValid(String value) {
        return value != null && value.matches("[A-Za-z0-9_-]{43}");
    }

    public static String challenge(String verifier) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required");
        }
    }
}
