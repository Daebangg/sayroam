package com.daebangg.sayroam.chat.domain.event;

import com.daebangg.sayroam.chat.domain.ChatMessage;

public record ChatMessageSent(Long messageId, Long roomId, Long senderId,
                              String clientMessageId, String content) {

    public static ChatMessageSent from(ChatMessage message) {
        return new ChatMessageSent(message.getId(), message.getRoomId(),
                message.getSenderId(), message.getClientMessageId(), message.getContent());
    }
}
