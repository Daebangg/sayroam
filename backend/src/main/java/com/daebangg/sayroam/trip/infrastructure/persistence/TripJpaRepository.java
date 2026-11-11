package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TripJpaRepository extends TripRepository, JpaRepository<Trip, Long> {

    @Override
    Optional<Trip> findById(Long tripId);
}
