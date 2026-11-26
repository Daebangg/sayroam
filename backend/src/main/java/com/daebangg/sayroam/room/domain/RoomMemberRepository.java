package com.daebangg.sayroam.room.domain;

import java.util.List;
import java.util.Optional;

public interface RoomMemberRepository {

    <S extends RoomMember> S save(S member);

    Optional<RoomMember> findByRoomIdAndUserId(Long roomId, Long userId);

    List<RoomMember> findAllByUserIdAndStatus(Long userId, RoomMemberStatus status);

    long countByRoomIdAndStatus(Long roomId, RoomMemberStatus status);
}
