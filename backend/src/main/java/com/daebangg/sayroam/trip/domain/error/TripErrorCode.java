package com.daebangg.sayroam.trip.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TripErrorCode implements ErrorCode {
    TRIP_PERIOD_REQUIRED(ErrorType.INVALID, "TRIP_PERIOD_REQUIRED", "Trip period is required");

    private final ErrorType type;
    private final String code;
    private final String message;
}
