package com.daebangg.sayroam.room.domain;

public interface RoomRepository {

    <S extends Room> S save(S room);

    // 조회하지 않고 ID만 가진 참조를 돌려준다. 이미 있는 방의 ID로만 부른다
    Room getReferenceById(Long roomId);
}
