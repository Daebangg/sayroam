package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends RoomRepository, JpaRepository<Room, Long> {

    @Override
    <S extends Room> S save(S room);
}
