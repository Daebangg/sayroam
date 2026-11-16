package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"tripPlaceId"})
public record AddTripPlaceResponse(@Schema(description = "추가된 여행 장소 ID", example = "31") Long tripPlaceId) {
}
