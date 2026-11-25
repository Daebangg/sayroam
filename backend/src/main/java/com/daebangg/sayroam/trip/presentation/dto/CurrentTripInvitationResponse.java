package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"code"})
public record CurrentTripInvitationResponse(
        @Schema(description = "지금 쓸 수 있는 초대 코드. 발급한 적이 없거나 만료됐으면 null이며, 이때 화면은 초대 링크 발급을 요청한다",
                example = "k3Xv9qLm2Tz8RwYb7NfA", types = {"string", "null"}) String code) {
}
