package com.daebangg.sayroam.chat.presentation.dto;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.ZoneOffset;

public record ChatMessageResponse(
        @Schema(description = "메시지 ID. 다음 조회의 afterId로 사용", example = "101") Long messageId,
        @Schema(description = "방 ID", example = "7") Long roomId,
        @Schema(description = "보낸 회원 ID. 시스템 메시지는 null", example = "1") Long senderId,
        @Schema(description = "클라이언트가 전송할 때 만든 UUID", example = "6f1c2b6e-8a57-4b7a-9d3e-2f4a1c7e9b10") String clientMessageId,
        @Schema(description = "메시지 본문", example = "안녕") String content,
        @Schema(description = "서버 저장 시각(UTC)", example = "2026-11-07T10:00:00Z") Instant createdAt) {

    public static ChatMessageResponse from(ChatMessageResult result) {
        return new ChatMessageResponse(result.messageId(), result.roomId(), result.senderId(),
                result.clientMessageId(), result.content(), result.createdAt().toInstant(ZoneOffset.UTC));
    }
}
