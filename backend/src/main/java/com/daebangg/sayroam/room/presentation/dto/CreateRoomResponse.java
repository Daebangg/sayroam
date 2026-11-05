package com.daebangg.sayroam.room.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"roomId"})
public record CreateRoomResponse(@Schema(description = "생성된 방 ID", example = "7") Long roomId) {
}
