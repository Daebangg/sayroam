package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"trips"})
public record TripListResponse(
        @Schema(description = "볼 수 있는 여행. 나중에 만든 여행부터 정렬")
        List<TripSummaryResponse> trips) {

    public static TripListResponse from(List<TripSummaryResult> results) {
        return new TripListResponse(results.stream()
                .map(TripSummaryResponse::from)
                .toList());
    }
}
