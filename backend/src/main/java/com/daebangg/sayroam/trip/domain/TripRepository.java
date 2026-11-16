package com.daebangg.sayroam.trip.domain;

import java.util.Optional;

public interface TripRepository {
    <S extends Trip> S save(S entity);

    Optional<Trip> findById(Long tripId);

    Optional<Trip> findByIdForUpdate(Long tripId);

    // 채팅방이 연결되면 참여 중인 멤버가, 없으면 만든 회원이 수정할 수 있다
    Optional<Trip> findAccessible(Long userId, Long tripId);
}
