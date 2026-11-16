package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.domain.TripPlace;
import com.daebangg.sayroam.trip.domain.TripPlaceRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripPlaceJpaRepository extends TripPlaceRepository, JpaRepository<TripPlace, Long> {
}
