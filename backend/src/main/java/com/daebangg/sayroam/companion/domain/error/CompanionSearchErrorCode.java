package com.daebangg.sayroam.companion.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionSearchErrorCode implements ErrorCode {
    COMPANION_SEARCH_NOT_FOUND(ErrorType.NOT_FOUND, "COMPANION_SEARCH_NOT_FOUND", "Companion search does not exist"),
    COMPANION_SEARCH_CLOSED(ErrorType.CONFLICT, "COMPANION_SEARCH_CLOSED", "Companion search is closed"),
    DESTINATION_UNAVAILABLE(ErrorType.INVALID, "DESTINATION_UNAVAILABLE", "Destination is unavailable");

    private final ErrorType type;
    private final String code;
    private final String message;
}
