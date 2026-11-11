package com.daebangg.sayroam.chat.application;

import com.daebangg.sayroam.chat.application.dto.ChatMessageResult;
import com.daebangg.sayroam.chat.domain.ChatMessage;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import com.daebangg.sayroam.chat.domain.event.ChatMessageSent;
import com.daebangg.sayroam.room.application.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Limit;
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

        return chatMessageRepository.findVisibleAfter(roomId, afterId, joinedAt, Limit.of(size))
                .stream()
                .map(ChatMessageResult::from)
                .toList();
    }

}
