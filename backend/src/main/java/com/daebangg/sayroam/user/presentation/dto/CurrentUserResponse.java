package com.daebangg.sayroam.user.presentation.dto;

import com.daebangg.sayroam.user.domain.Role;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentUserResponse(
        @Schema(example = "1", requiredMode = Schema.RequiredMode.REQUIRED) long userId,
        @Schema(example = "user@example.com", types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String email,
        @Schema(example = "여행자", requiredMode = Schema.RequiredMode.REQUIRED) String nickname,
        @Schema(types = {"string", "null"}, requiredMode = Schema.RequiredMode.REQUIRED) String profileImageUrl,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Role role) {
}
