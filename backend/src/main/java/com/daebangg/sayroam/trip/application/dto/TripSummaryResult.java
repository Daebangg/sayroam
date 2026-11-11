package com.daebangg.sayroam.trip.application.dto;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.trip.domain.Trip;

public record TripSummaryResult(Long tripId, String title, TravelPeriod period, Long roomId) {

    public static TripSummaryResult from(Trip trip) {
        return new TripSummaryResult(trip.getId(), trip.getTitle(), trip.getPeriod(), trip.getRoomId());
    }
}
