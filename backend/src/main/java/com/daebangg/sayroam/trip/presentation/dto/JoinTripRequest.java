package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(requiredProperties = {"invitationCode"})
public record JoinTripRequest(
        @Schema(description = "초대 링크의 코드", example = "k3Xv9qLm2Tz8RwYb7NfA")
        @NotBlank String invitationCode) {
}
