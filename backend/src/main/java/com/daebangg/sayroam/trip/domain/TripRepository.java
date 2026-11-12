package com.daebangg.sayroam.trip.domain;

import java.util.Optional;

public interface TripRepository {
    <S extends Trip> S save(S entity);

    Optional<Trip> findById(Long tripId);

    Optional<Trip> findByIdForUpdate(Long tripId);
}
