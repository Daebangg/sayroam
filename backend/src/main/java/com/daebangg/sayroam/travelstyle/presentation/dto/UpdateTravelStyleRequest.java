package com.daebangg.sayroam.travelstyle.presentation.dto;

import com.daebangg.sayroam.travelstyle.application.dto.UpdateTravelStyleCommand;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "전달한 문항의 답변만 수정합니다. score가 null이면 그 문항의 답변을 삭제합니다.",
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record UpdateTravelStyleRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Answer> answers) {

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UpdateTravelStyleRequest from(JsonNode body) {
        if (body == null || !body.isObject() || body.size() != 1 || !body.path("answers").isArray()) {
            throw new IllegalArgumentException("Travel style update requires an answers array");
        }
        List<Answer> answers = new ArrayList<>();
        for (JsonNode answer : body.path("answers")) {
            answers.add(readAnswer(answer));
        }
        return new UpdateTravelStyleRequest(answers);
    }

    private static Answer readAnswer(JsonNode answer) {
        JsonNode code = answer.path("code");
        JsonNode score = answer.path("score");
        if (!answer.isObject() || answer.size() != 2 || !code.isString() || code.stringValue().isBlank()
                || (!score.isNull() && (!score.isIntegralNumber() || !score.canConvertToInt()))) {
            throw new IllegalArgumentException("An answer requires a question code and an integer or null score");
        }
        return new Answer(code.stringValue(), score.isNull() ? null : score.intValue());
    }

    public UpdateTravelStyleCommand toCommand() {
        return new UpdateTravelStyleCommand(answers.stream()
                .map(answer -> new UpdateTravelStyleCommand.Answer(answer.code(), answer.score()))
                .toList());
    }

    @Schema(additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
    public record Answer(
            @Schema(example = "planning", requiredMode = Schema.RequiredMode.REQUIRED) String code,
            @Schema(types = {"integer", "null"}, format = "int32", minimum = "" + UserTravelStyle.MIN_SCORE,
                    maximum = "" + UserTravelStyle.MAX_SCORE, requiredMode = Schema.RequiredMode.REQUIRED)
            @Nullable Integer score) {
    }
}
