package com.daebangg.sayroam.common.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelPeriodErrorCode implements ErrorCode {
    TRAVEL_PERIOD_END_BEFORE_START(ErrorType.INVALID, "TRAVEL_PERIOD_END_BEFORE_START",
            "End date must be on or after start date");

    private final ErrorType type;
    private final String code;
    private final String message;
}
