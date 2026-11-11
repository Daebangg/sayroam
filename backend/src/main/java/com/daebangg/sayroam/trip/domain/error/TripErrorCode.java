package com.daebangg.sayroam.trip.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TripErrorCode implements ErrorCode {
    TRIP_PERIOD_REQUIRED(ErrorType.INVALID, "TRIP_PERIOD_REQUIRED", "Trip period is required"),
    TRIP_NOT_FOUND(ErrorType.NOT_FOUND, "TRIP_NOT_FOUND", "Trip does not exist"),
    TRIP_ROOM_ALREADY_LINKED(ErrorType.CONFLICT, "TRIP_ROOM_ALREADY_LINKED", "Trip is already linked to a chat room");

    private final ErrorType type;
    private final String code;
    private final String message;
}
