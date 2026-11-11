package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripJpaRepository extends TripRepository, JpaRepository<Trip, Long> {
}
