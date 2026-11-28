package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.daebangg.sayroam.companion.domain.TravelStyleMatch;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

@Schema(description = "모집에만 저장하는 4문항 점수. 각 점수는 1~5의 정수입니다.",
        requiredProperties = {"planning", "daily-pace", "time-together", "spending"},
        additionalProperties = Schema.AdditionalPropertiesValue.FALSE)
public record RecruitmentTravelStyleRequest(
        @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE) @Nullable Integer planning,
        @JsonProperty("daily-pace") @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE) @Nullable Integer dailyPace,
        @JsonProperty("time-together") @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE) @Nullable Integer timeTogether,
        @Schema(minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE) @Nullable Integer spending
) {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static RecruitmentTravelStyleRequest from(JsonNode body) {
        if (!body.isObject() || !TravelStyleMatch.QUESTION_CODES.containsAll(body.propertyNames())) {
            throw new IllegalArgumentException("Travel style requires four known question codes");
        }
        return new RecruitmentTravelStyleRequest(score(body.path("planning")), score(body.path("daily-pace")),
                score(body.path("time-together")), score(body.path("spending")));
    }

    private static Integer score(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new IllegalArgumentException("Travel style scores must be integers");
        }
        return value.intValue();
    }

    public Map<String, Integer> answers() {
        Map<String, Integer> answers = new HashMap<>();
        answers.put("planning", planning);
        answers.put("daily-pace", dailyPace);
        answers.put("time-together", timeTogether);
        answers.put("spending", spending);
        return answers;
    }
}
