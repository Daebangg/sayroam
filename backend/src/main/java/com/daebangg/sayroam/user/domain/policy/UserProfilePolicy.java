package com.daebangg.sayroam.user.domain.policy;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.user.domain.error.UserErrorCode;
import com.daebangg.sayroam.global.validation.InputText;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Year;
import java.time.LocalDate;
import java.util.Set;

/** 회원 생성과 프로필 변경에서 공통으로 사용하는 정규화·검증 규칙. */
public final class UserProfilePolicy {
    public static final int MAX_NICKNAME_LENGTH = 50;
    public static final String NICKNAME_PATTERN = "^[^\\p{C}\\p{Zl}\\p{Zp}]+$";
    public static final int MAX_PROFILE_IMAGE_URL_LENGTH = 500;

    private static final Set<String> AVATAR_PRESETS = Set.of(
            "coast", "sand", "dusk", "sea", "traveler", "sunrise", "poet", "explorer",
            "alien", "dog", "rabbit", "cat", "sunset", "twilight", "night", "flight");

    private UserProfilePolicy() {
    }

    public static String normalizeNickname(String nickname) {
        String normalized = InputText.trim(nickname);
        if (normalized == null || normalized.isEmpty()) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_REQUIRED);
        }

        if (normalized.length() > MAX_NICKNAME_LENGTH) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_TOO_LONG);
        }

        if (!normalized.matches(NICKNAME_PATTERN)) {
            throw new BusinessException(UserErrorCode.USER_NICKNAME_INVALID);
        }

        return normalized;
    }

    public static String normalizeProfileImageUrl(String profileImageUrl) {
        if (profileImageUrl == null) {
            return null;
        }

        String normalized = profileImageUrl.strip();
        if (normalized.length() > MAX_PROFILE_IMAGE_URL_LENGTH) {
            throw new BusinessException(UserErrorCode.USER_PROFILE_IMAGE_URL_TOO_LONG);
        }

        if (normalized.startsWith("/brand/avatars/") && normalized.endsWith(".svg")) {
            String preset = normalized.substring("/brand/avatars/".length(), normalized.length() - 4);
            if (AVATAR_PRESETS.contains(preset)) return normalized;
        }

        if (normalized.matches("/images/profiles/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg")) {
            return normalized;
        }

        URI uri;
        try {
            uri = new URI(normalized);
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

        return normalized;
    }

    public static LocalDate validateBirthDate(LocalDate birthDate) {
        if (birthDate != null && (birthDate.getYear() < 1 || birthDate.isAfter(LocalDate.now()))) {
            throw new BusinessException(UserErrorCode.USER_BIRTH_DATE_INVALID);
        }
        return birthDate;
    }

    public static Integer validateBirthYear(Integer birthYear) {
        if (birthYear == null) {
            return null;
        }

        if (birthYear <= 0 || birthYear > Year.now().getValue()) {
            throw new BusinessException(UserErrorCode.USER_BIRTH_YEAR_INVALID);
        }

        return birthYear;
    }
}
