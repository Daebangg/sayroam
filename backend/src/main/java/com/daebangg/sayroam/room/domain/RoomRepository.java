package com.daebangg.sayroam.room.domain;

import java.util.Optional;

public interface RoomRepository {

    <S extends Room> S save(S room);

    Optional<Room> findById(Long roomId);
}
