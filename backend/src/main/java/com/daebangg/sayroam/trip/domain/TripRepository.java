package com.daebangg.sayroam.trip.domain;


public interface TripRepository {
    <S extends Trip> S save(S entity);
}
