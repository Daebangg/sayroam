package com.daebangg.sayroam.chat.application.dto;

import com.daebangg.sayroam.chat.domain.ChatMessage;

import java.time.LocalDateTime;

public record ChatMessageResult(Long messageId, Long roomId, Long senderId,
                                String clientMessageId, String content, LocalDateTime createdAt) {

    public static ChatMessageResult from(ChatMessage message) {
        return new ChatMessageResult(message.getId(), message.getRoomId(), message.getSenderId(),
                message.getClientMessageId(), message.getContent(), message.getCreatedAt());
    }
}
