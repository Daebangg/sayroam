package com.daebangg.sayroam.chat.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"unreadCount"})
public record ChatMessageSummaryResponse(
        @Schema(description = "읽은 위치 이후 다른 회원이 보낸 메시지 개수. 입장 전 메시지와 내가 보낸 메시지는 세지 않음",
                types = {"integer"}, format = "int64", minimum = "0", example = "3")
        long unreadCount
) {
}
