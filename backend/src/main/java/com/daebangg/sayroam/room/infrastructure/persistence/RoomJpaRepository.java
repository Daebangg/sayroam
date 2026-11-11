package com.daebangg.sayroam.room.infrastructure.persistence;

import com.daebangg.sayroam.room.domain.Room;
import com.daebangg.sayroam.room.domain.RoomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomJpaRepository extends RoomRepository, JpaRepository<Room, Long> {

    // domain과 CrudRepository의 save를 하나로 합쳐 이 타입으로 호출할 때의 모호성을 없앤다
    @Override
    <S extends Room> S save(S room);
}
