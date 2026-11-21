package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UpdateTripRequest(
        @Schema(description = "바꿀 여행 이름. 바꿀 때만 보낸다. 보내지 않거나 null이면 그대로 두고, 빈 문자열이면 이름을 지운다. "
                + "앞뒤 공백을 지운 뒤 최대 30자(이모지도 한 글자)",
                example = "부산 여행", maxLength = 30, types = {"string", "null"})
        String title) {
}
