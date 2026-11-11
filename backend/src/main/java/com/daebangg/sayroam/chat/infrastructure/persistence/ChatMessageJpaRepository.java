package com.daebangg.sayroam.chat.infrastructure.persistence;

import com.daebangg.sayroam.chat.domain.ChatMessage;
import com.daebangg.sayroam.chat.domain.ChatMessageRepository;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageJpaRepository extends ChatMessageRepository, JpaRepository<ChatMessage, Long> {
    @Override
    <S extends ChatMessage> S save(S message);

    @Override
    default List<ChatMessage> findVisibleAfter(Long roomId, Long afterId, LocalDateTime joinedAt, int size) {
        return findVisibleAfter(roomId, afterId, joinedAt, Limit.of(size));
    }

    @Query("""
            SELECT m
            FROM ChatMessage m
            WHERE m.roomId = :roomId AND m.id > :afterId AND m.createdAt >= :joinedAt
            ORDER BY m.id ASC
            """)
    List<ChatMessage> findVisibleAfter(Long roomId, Long afterId, LocalDateTime joinedAt, Limit limit);
}
