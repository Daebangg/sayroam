package com.daebangg.sayroam.room.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoomMemberRepository extends JpaRepository<RoomMember, Long> {

    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);
}
