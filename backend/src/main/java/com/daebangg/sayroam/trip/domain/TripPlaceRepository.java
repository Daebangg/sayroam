package com.daebangg.sayroam.trip.domain;

public interface TripPlaceRepository {
    <S extends TripPlace> S save(S tripPlace);
}
