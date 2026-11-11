package com.daebangg.sayroam.room.domain;

import java.util.Optional;

public interface RoomInvitationRepository {

    <S extends RoomInvitation> S save(S invitation);

    Optional<RoomInvitation> findByCode(String code);
}
