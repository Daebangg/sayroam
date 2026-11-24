package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult;
import com.daebangg.sayroam.room.application.port.out.RoomMemberQueryRepository;
import com.daebangg.sayroam.room.domain.RoomMember;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RoomMemberQueryJpaRepository extends RoomMemberQueryRepository, Repository<RoomMember, Long> {
    @Override
    @Query("""
            select new com.daebangg.sayroam.room.application.dto.RoomMemberProfileResult(
                       m.userId, u.nickname, u.profileImageUrl, m.status)
            from RoomMember m
            join User u on u.id = m.userId
            where m.room.id = :roomId
            """)
    List<RoomMemberProfileResult> findProfiles(@Param("roomId") Long roomId);
}
