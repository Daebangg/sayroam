package com.daebangg.sayroam.trip.infrastructure.persistence;

import com.daebangg.sayroam.trip.domain.Trip;
import com.daebangg.sayroam.trip.domain.TripRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TripJpaRepository extends TripRepository, JpaRepository<Trip, Long> {
    @Override
    <S extends Trip> S save(S entity);

    @Override
    Optional<Trip> findById(Long tripId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :tripId")
    Optional<Trip> findByIdForUpdate(@Param("tripId") Long tripId);
}
