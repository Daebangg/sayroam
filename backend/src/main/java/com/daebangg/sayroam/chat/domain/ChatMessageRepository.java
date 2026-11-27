package com.daebangg.sayroam.chat.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository {

    <S extends ChatMessage> S save(S message);

    Optional<ChatMessage> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    boolean existsByIdAndRoomId(Long id, Long roomId);

    List<ChatMessage> findVisibleAfter(Long roomId, Long afterId, LocalDateTime joinedAt, int size);

    long countUnread(Long roomId, Long userId, Long afterId, LocalDateTime joinedAt);

    long findLastMessageId(Long roomId);
}
