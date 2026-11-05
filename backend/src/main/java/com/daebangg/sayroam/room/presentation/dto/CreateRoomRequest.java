package com.daebangg.sayroam.room.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CreateRoomRequest(
        @Schema(description = "방 이름. 선택값이며 앞뒤 공백 제거 후 20자 이내. 공백뿐이면 이름 없는 방으로 저장", example = "제주 3박 4일", maxLength = 20, types = {"string", "null"})
        String name) {
}
