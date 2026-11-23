package com.daebangg.sayroam.trip.application.dto;

import java.time.LocalDate;

public record CreateTripCommand(Long destinationId, LocalDate startDate, LocalDate endDate, String title) {
}
