package com.daebangg.sayroam.trip.application.dto;

import java.math.BigDecimal;

public record AddTripCustomPlaceCommand(String name, BigDecimal latitude, BigDecimal longitude,
                                        Integer day, String memo) {
}
