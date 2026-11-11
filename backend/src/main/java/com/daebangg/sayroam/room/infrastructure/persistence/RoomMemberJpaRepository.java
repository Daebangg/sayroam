package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.domain.RoomMember;
import com.daebangg.sayroam.room.domain.RoomMemberRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomMemberJpaRepository extends RoomMemberRepository, JpaRepository<RoomMember, Long> {

    @Override
    <S extends RoomMember> S save(S member);
}
