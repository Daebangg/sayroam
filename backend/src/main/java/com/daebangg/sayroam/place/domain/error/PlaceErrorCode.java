package com.daebangg.sayroam.place.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PlaceErrorCode implements ErrorCode {
    PLACE_NAME_REQUIRED(ErrorType.INVALID, "PLACE_NAME_REQUIRED", "Place name is required"),
    PLACE_NAME_TOO_LONG(ErrorType.INVALID, "PLACE_NAME_TOO_LONG", "Place name is too long"),
    PLACE_NOT_FOUND(ErrorType.NOT_FOUND, "PLACE_NOT_FOUND", "Place does not exist"),
    PLACE_SEARCH_DISABLED(ErrorType.UNAVAILABLE, "PLACE_SEARCH_DISABLED", "Place search is not available"),
    PLACE_SEARCH_FAILED(ErrorType.EXTERNAL_FAILURE, "PLACE_SEARCH_FAILED", "Place search provider failed; try again later");

    private final ErrorType type;
    private final String code;
    private final String message;
}
