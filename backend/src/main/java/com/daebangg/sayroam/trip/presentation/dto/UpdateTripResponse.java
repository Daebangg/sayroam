package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.trip.application.dto.TripTitleResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"tripId", "title"})
public record UpdateTripResponse(
        @Schema(description = "여행 ID", example = "12")
        Long tripId,
        @Schema(description = "저장된 여행 이름. 앞뒤 공백을 지운 값이며 비웠으면 null", example = "부산 여행",
                types = {"string", "null"})
        String title) {

    public static UpdateTripResponse from(TripTitleResult result) {
        return new UpdateTripResponse(result.tripId(), result.title());
    }
}
