package com.daebangg.sayroam.chat.domain;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findBySenderIdAndClientMessageId(Long senderId, String clientMessageId);

    @Query("""
            SELECT m 
            FROM ChatMessage m
            WHERE m.roomId = :roomId AND m.id > :afterId AND m.createdAt >= :joinedAt
            ORDER BY m.id ASC 
            """)
    List<ChatMessage> findVisibleAfter(Long roomId, Long afterId, LocalDateTime joinedAt, Limit limit);
}
