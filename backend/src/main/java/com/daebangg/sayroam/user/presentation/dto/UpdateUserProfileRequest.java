package com.daebangg.sayroam.user.presentation.dto;

import com.daebangg.sayroam.user.application.command.UpdateUserProfileCommand;
import com.daebangg.sayroam.user.application.command.UpdateUserProfileCommand.FieldUpdate;
import com.daebangg.sayroam.user.domain.policy.UserProfilePolicy;
import com.daebangg.sayroam.user.domain.type.Gender;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.Set;

@Getter
@Schema(description = "생략한 필드는 유지합니다. 선택 정보의 null은 삭제를 의미합니다.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public final class UpdateUserProfileRequest {
    private static final Set<String> ALLOWED_FIELDS =
            Set.of("nickname", "profileImageUrl", "gender", "birthYear");

    @Schema(description = "앞뒤 공백 제거 후 1~50자. 제어 문자와 null 불가",
            example = "제주 여행자", minLength = 1, maxLength = UserProfilePolicy.MAX_NICKNAME_LENGTH,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final String nickname;

    @Schema(description = "HTTP(S) 이미지 URL. null이면 제거",
            types = {"string", "null"}, maxLength = UserProfilePolicy.MAX_PROFILE_IMAGE_URL_LENGTH,
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final String profileImageUrl;

    @Schema(description = "MALE 또는 FEMALE. null이면 미설정",
            enumAsRef = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final @Nullable Gender gender;

    @Schema(description = "양의 정수이며 현재 연도 이하. null이면 미설정",
            types = {"integer", "null"}, format = "int32", minimum = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private final Integer birthYear;

    @JsonIgnore
    @Getter(AccessLevel.NONE)
    @Schema(hidden = true)
    private final Set<String> providedFields;

    /** JSON 원문으로 전달 여부와 타입을 확인해 생략과 null을 구분한다. */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public UpdateUserProfileRequest(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Profile update must be a JSON object");
        }

        providedFields = Set.copyOf(body.propertyNames());
        if (!ALLOWED_FIELDS.containsAll(providedFields)) {
            throw new IllegalArgumentException(
                    "Only nickname, profileImageUrl, gender and birthYear can be updated");
        }

        nickname = nullableString(body.get("nickname"), "nickname");
        profileImageUrl = nullableString(body.get("profileImageUrl"), "profileImageUrl");
        gender = nullableGender(body.get("gender"));
        birthYear = nullableBirthYear(body.get("birthYear"));
    }

    public UpdateUserProfileCommand toCommand() {
        return new UpdateUserProfileCommand(
                fieldUpdate("nickname", nickname),
                fieldUpdate("profileImageUrl", profileImageUrl),
                fieldUpdate("gender", gender),
                fieldUpdate("birthYear", birthYear));
    }

    private <T> FieldUpdate<T> fieldUpdate(String field, T value) {
        return providedFields.contains(field) ? FieldUpdate.of(value) : FieldUpdate.absent();
    }

    private static String nullableString(JsonNode node, String field) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isString()) {
            throw new IllegalArgumentException(field + " must be a string or null");
        }
        return node.stringValue();
    }

    private static Gender nullableGender(JsonNode node) {
        String value = nullableString(node, "gender");
        if (value == null) {
            return null;
        }
        return switch (value) {
            case "MALE" -> Gender.MALE;
            case "FEMALE" -> Gender.FEMALE;
            default -> throw new IllegalArgumentException("gender must be MALE, FEMALE or null");
        };
    }

    private static Integer nullableBirthYear(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isIntegralNumber() || !node.canConvertToInt()) {
            throw new IllegalArgumentException("birthYear must be a 32-bit integer or null");
        }
        return node.intValue();
    }
}
