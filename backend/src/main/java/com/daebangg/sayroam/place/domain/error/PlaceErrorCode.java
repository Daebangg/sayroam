package com.daebangg.sayroam.place.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
    PLACE_NAME_REQUIRED(ErrorType.INVALID, "PLACE_NAME_REQUIRED", "Place name is required"),
    PLACE_NAME_TOO_LONG(ErrorType.INVALID, "PLACE_NAME_TOO_LONG", "Place name is too long");

    private final ErrorType type;
    private final String code;
    private final String message;
}
