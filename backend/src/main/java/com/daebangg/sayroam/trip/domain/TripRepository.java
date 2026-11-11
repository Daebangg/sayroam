package com.daebangg.sayroam.trip.domain;

import java.util.List;
import java.util.Optional;

public interface TripRepository {
    <S extends Trip> S save(S entity);

    Optional<Trip> findById(Long tripId);

    List<Trip> findAllByCreatedByIdOrderByIdDesc(Long createdById);
}
