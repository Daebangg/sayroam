package com.daebangg.sayroam.chat.presentation.dto;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;

public record ChatMessageResponse(Long messageId, Long roomId,
                                  Long senderId, String clientMessageId, String content) {

    public static ChatMessageResponse from(ChatMessageResult result) {
        return new ChatMessageResponse(result.messageId(), result.roomId(),
                result.senderId(), result.clientMessageId(), result.content());
    }
}
