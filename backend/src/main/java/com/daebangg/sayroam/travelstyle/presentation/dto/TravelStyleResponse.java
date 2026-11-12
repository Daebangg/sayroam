package com.daebangg.sayroam.travelstyle.presentation.dto;

import com.daebangg.sayroam.travelstyle.application.dto.TravelStyleItemResult;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.util.List;

public record TravelStyleResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Item> items) {

    public static TravelStyleResponse from(List<TravelStyleItemResult> results) {
        return new TravelStyleResponse(results.stream().map(Item::from).toList());
    }

    public record Item(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String code,
            @Schema(description = "미응답이면 null", types = {"integer", "null"}, format = "int32",
                    minimum = "" + UserTravelStyle.MIN_SCORE, maximum = "" + UserTravelStyle.MAX_SCORE,
                    requiredMode = Schema.RequiredMode.REQUIRED)
            @Nullable Integer score) {

        private static Item from(TravelStyleItemResult result) {
            return new Item(result.code(), result.score());
        }
    }
}
