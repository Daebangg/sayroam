package com.daebangg.sayroam.chat.presentation.dto;

import com.daebangg.sayroam.chat.application.dto.ChatMessageSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"unreadCount", "lastMessageId"})
public record ChatMessageSummaryResponse(
        @Schema(description = "읽은 위치 이후 다른 회원이 보낸 메시지 개수. 입장 전 메시지와 내가 보낸 메시지는 세지 않음",
                types = {"integer"}, format = "int64", minimum = "0", example = "3")
        long unreadCount,
        @Schema(description = "개수를 센 시점의 방의 마지막 메시지 ID. 이보다 뒤에 받은 메시지는 개수에 포함되지 않음. 메시지가 없으면 0",
                types = {"integer"}, format = "int64", minimum = "0", example = "10")
        long lastMessageId
) {
    public static ChatMessageSummaryResponse from(ChatMessageSummaryResult result) {
        return new ChatMessageSummaryResponse(result.unreadCount(), result.lastMessageId());
    }
}
