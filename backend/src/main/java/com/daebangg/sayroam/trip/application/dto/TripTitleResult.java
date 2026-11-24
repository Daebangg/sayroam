package com.daebangg.sayroam.trip.application.dto;

import com.daebangg.sayroam.trip.domain.Trip;

public record TripTitleResult(Long tripId, String title) {

    public static TripTitleResult from(Trip trip) {
        return new TripTitleResult(trip.getId(), trip.getTitle());
    }
}
