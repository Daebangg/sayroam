package com.daebangg.sayroam.trip.application.dto;

import com.daebangg.sayroam.common.domain.TravelPeriod;

import java.math.BigDecimal;

public record TripSummaryResult(Long tripId, String title, TravelPeriod period, Long roomId,
                                Long destinationId, String destinationName,
                                BigDecimal boundsSouth, BigDecimal boundsWest,
                                BigDecimal boundsNorth, BigDecimal boundsEast) {
}
