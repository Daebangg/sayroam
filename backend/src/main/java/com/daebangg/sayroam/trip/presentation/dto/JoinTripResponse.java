package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"tripId"})
public record JoinTripResponse(@Schema(description = "참여한 여행 ID", example = "12") Long tripId) {
}
