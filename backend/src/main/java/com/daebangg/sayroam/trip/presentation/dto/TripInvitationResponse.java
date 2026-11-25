package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"code"})
public record TripInvitationResponse(
        @Schema(description = "초대 링크에 넣는 코드. 링크를 받은 사람이 이 코드로 여행에 참여한다",
                example = "k3Xv9qLm2Tz8RwYb7NfA") String code) {
}
