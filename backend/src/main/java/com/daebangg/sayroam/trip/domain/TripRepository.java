package com.daebangg.sayroam.trip.domain;

import java.util.Optional;

public interface TripRepository {
    <S extends Trip> S save(S entity);

    Optional<Trip> findById(Long tripId);

    Optional<Trip> findByIdForUpdate(Long tripId);

    Optional<Trip> findByInvitationCode(String invitationCode);

    Optional<Trip> findAccessible(Long userId, Long tripId);

    // 수정할 때 쓴다. 다른 요청이 커밋한 방 연결 같은 최신 값을 읽고, 수정이 끝날 때까지 행을 잠근다
    Optional<Trip> findAccessibleForUpdate(Long userId, Long tripId);
}
