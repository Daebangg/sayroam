package com.daebangg.sayroam.chat.domain;

import com.daebangg.sayroam.chat.domain.error.ChatErrorCode;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Entity
@Table(name = "chat_message",
        uniqueConstraints = @UniqueConstraint(name = "uk_chat_message_sender_client",
                columnNames = {"sender_id", "client_message_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseTimeEntity {
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "room_id", nullable = false)
    private Long roomId;

    // 시스템 메시지는 NULL
    @Getter
    @Column(name = "sender_id")
    private Long senderId;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageType type;

    @Getter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Getter
    @Column(name = "client_message_id", length = 36)
    private String clientMessageId;

    private ChatMessage(Long roomId, Long senderId, ChatMessageType type, String clientMessageId, String content) {
        this.roomId = roomId;
        this.senderId = senderId;
        this.type = type;
        this.clientMessageId = clientMessageId;
        this.content = content;
    }

    public static ChatMessage text(Long roomId, Long senderId, String clientMessageId, String content) {
        if (clientMessageId == null || !UUID_PATTERN.matcher(clientMessageId).matches()) {
            throw new BusinessException(ChatErrorCode.CHAT_CLIENT_MESSAGE_ID_INVALID);
        }

        validateContent(content);

        return new ChatMessage(roomId, senderId, ChatMessageType.TEXT, clientMessageId, content);
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ChatErrorCode.CHAT_CONTENT_BLANK);
        }

        if (content.codePointCount(0, content.length()) > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ChatErrorCode.CHAT_CONTENT_TOO_LONG);
        }
    }
}
