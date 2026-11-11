package com.daebangg.sayroam.user.presentation.dto;

import com.daebangg.sayroam.user.application.result.UserProfileResult;
import com.daebangg.sayroam.user.domain.type.Gender;
import com.daebangg.sayroam.user.domain.type.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public record CurrentUserResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) long userId,
        @Schema(example = "user@example.com", types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(example = "여행자", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String profileImageUrl,
        @Schema(description = "미설정이면 null", enumAsRef = true,
                requiredMode = Schema.RequiredMode.REQUIRED) @Nullable Gender gender,
        @Schema(description = "미설정이면 null", types = {"integer", "null"},
                requiredMode = Schema.RequiredMode.REQUIRED) Integer birthYear,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Role role,
        @Schema(description = "가입 시각(UTC)", example = "2026-11-10T12:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED) Instant createdAt,
        @Schema(description = "생일. 연도만 등록된 기존 계정은 null", types = {"string", "null"}, format = "date", requiredMode = Schema.RequiredMode.REQUIRED) LocalDate birthDate) {

    public static CurrentUserResponse from(UserProfileResult profile) {
        return new CurrentUserResponse(profile.userId(), profile.email(), profile.nickname(),
                profile.profileImageUrl(), profile.gender(), profile.birthYear(), profile.role(),
                profile.createdAt().toInstant(ZoneOffset.UTC), profile.birthDate());
    }
}
