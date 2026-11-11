package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"roomId"})
public record CreateTripRoomResponse(@Schema(description = "생성해 여행에 연결한 방 ID", example = "7") Long roomId) {
}
