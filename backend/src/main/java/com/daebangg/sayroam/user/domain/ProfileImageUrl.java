package com.daebangg.sayroam.user.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public record ProfileImageUrl(String value) {
    public static final int MAX_LENGTH = 500;
    public static final String UPLOAD_PATH_PREFIX = "/images/profiles/";

    private static final Set<String> AVATAR_PRESETS = Set.of(
            "coast", "sand", "dusk", "sea", "traveler", "sunrise", "poet", "explorer",
            "alien", "dog", "rabbit", "cat", "sunset", "twilight", "night", "flight");

    public ProfileImageUrl {
        if (value == null) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID);
        }
        value = value.strip();
        validate(value);
    }

    private static void validate(String value) {
        if (value.length() > MAX_LENGTH) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_TOO_LONG);
        }

        if (value.startsWith("/brand/avatars/") && value.endsWith(".svg")) {
            String preset = value.substring("/brand/avatars/".length(), value.length() - 4);
            if (AVATAR_PRESETS.contains(preset)) return;
        }

        if (value.matches(UPLOAD_PATH_PREFIX + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")) {
            return;
        }

        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException exception) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID, exception);
        }

        boolean validScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        boolean validPort = uri.getPort() == -1
                || (uri.getPort() >= 1 && uri.getPort() <= 65535);
        if (!validScheme || uri.getHost() == null || uri.getUserInfo() != null || !validPort) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_INVALID);
        }
    }
}
