package com.daebangg.sayroam.room.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"code"})
public record CreateRoomInvitationResponse(
        @Schema(description = "초대 코드", example = "k3Xv9qLm2Tz8RwYb7NfA") String code) {
}
