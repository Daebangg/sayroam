package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RoomMemberJpaRepository extends RoomMemberRepository, JpaRepository<RoomMember, Long> {

    @Override
    <S extends RoomMember> S save(S member);

    @Override
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RoomMember m
            SET m.lastReadMessageId = :messageId
            WHERE m.room.id = :roomId AND m.userId = :userId
                AND m.status = com.daebangg.sayroam.room.domain.RoomMemberStatus.JOINED
                AND (m.lastReadMessageId IS NULL OR m.lastReadMessageId < :messageId)
            """)
    int advanceLastRead(Long roomId, Long userId, Long messageId);
}
