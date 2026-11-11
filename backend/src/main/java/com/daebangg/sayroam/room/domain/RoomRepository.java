package com.daebangg.sayroam.room.domain;

public interface RoomRepository {

    <S extends Room> S save(S room);
}
