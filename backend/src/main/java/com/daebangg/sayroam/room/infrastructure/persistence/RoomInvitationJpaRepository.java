package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.domain.RoomInvitation;
import com.daebangg.sayroam.room.domain.RoomInvitationRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomInvitationJpaRepository extends RoomInvitationRepository, JpaRepository<RoomInvitation, Long> {

    @Override
    <S extends RoomInvitation> S save(S invitation);
}
