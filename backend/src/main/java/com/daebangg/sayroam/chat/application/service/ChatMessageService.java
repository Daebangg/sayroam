package com.daebangg.sayroam.chat.application.service;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.application.dto.ChatMessageSummaryResult;
import com.daebangg.sayroam.chat.domain.ChatMessage;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import com.daebangg.sayroam.chat.domain.error.ChatErrorCode;
import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.room.application.dto.ReadPositionResult;
import com.daebangg.sayroam.room.application.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final RoomService roomService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChatMessageResult send(Long roomId, Long senderId, String clientMessageId, String content) {
        roomService.ensureJoined(roomId, senderId);

        Optional<ChatMessage> existing = chatMessageRepository.findBySenderIdAndClientMessageId(senderId, clientMessageId);
        if (existing.isPresent()) {
            return ChatMessageResult.from(existing.get());
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.text(roomId, senderId, clientMessageId, content));
        eventPublisher.publishEvent(ChatMessageSent.from(message));
        return ChatMessageResult.from(message);
    }

    public List<ChatMessageResult> findAfter(Long roomId, Long userId, Long afterId, int size) {
        LocalDateTime joinedAt = roomService.getJoinedAt(roomId, userId);

        return chatMessageRepository.findVisibleAfter(roomId, afterId, joinedAt, size)
                .stream()
                .map(ChatMessageResult::from)
                .toList();
    }

    @Transactional
    public void advanceLastRead(Long roomId, Long userId, Long messageId) {
        roomService.ensureJoined(roomId, userId);
        if (!chatMessageRepository.existsByIdAndRoomId(messageId, roomId)) {
            throw new BusinessException(ChatErrorCode.CHAT_MESSAGE_NOT_FOUND);
        }
        roomService.advanceLastRead(roomId, userId, messageId);
    }

    public ChatMessageSummaryResult getSummary(Long roomId, Long userId) {
        ReadPositionResult position = roomService.getReadPosition(roomId, userId);
        long lastMessageId = chatMessageRepository.findLastMessageId(roomId);
        long unreadCount = chatMessageRepository.countUnread(
                roomId, userId, position.lastReadMessageId(), lastMessageId, position.joinedAt());
        return new ChatMessageSummaryResult(unreadCount, lastMessageId);
    }

}
