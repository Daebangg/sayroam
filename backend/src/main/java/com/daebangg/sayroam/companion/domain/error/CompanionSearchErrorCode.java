package com.daebangg.sayroam.companion.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CompanionSearchErrorCode implements ErrorCode {
    COMPANION_CANDIDATE_UNAVAILABLE(ErrorType.CONFLICT, "COMPANION_CANDIDATE_UNAVAILABLE", "Candidate is unavailable"),
    COMPANION_REQUEST_NOT_FOUND(ErrorType.NOT_FOUND, "COMPANION_REQUEST_NOT_FOUND", "Companion request does not exist"),
    COMPANION_REQUEST_ALREADY_ANSWERED(ErrorType.CONFLICT, "COMPANION_REQUEST_ALREADY_ANSWERED", "Request has already been answered"),
    COMPANION_SEARCH_LIMIT_EXCEEDED(ErrorType.CONFLICT, "COMPANION_SEARCH_LIMIT_EXCEEDED",
            "A maximum of 3 active companion searches is allowed. Close an existing search before creating or reopening one"),
    TRAVEL_STYLE_REQUIRED(ErrorType.CONFLICT, "TRAVEL_STYLE_REQUIRED", "Complete all four travel style questions first"),
    COMPANION_SEARCH_NOT_FOUND(ErrorType.NOT_FOUND, "COMPANION_SEARCH_NOT_FOUND", "Companion search does not exist"),
    COMPANION_SEARCH_CLOSED(ErrorType.CONFLICT, "COMPANION_SEARCH_CLOSED", "Companion search is closed"),
    COMPANION_SEARCH_DESTINATION_REQUIRED(ErrorType.CONFLICT, "COMPANION_SEARCH_DESTINATION_REQUIRED",
            "Select a destination before finding companion candidates"),
    DESTINATION_UNAVAILABLE(ErrorType.INVALID, "DESTINATION_UNAVAILABLE", "Destination is unavailable");

    private final ErrorType type;
    private final String code;
    private final String message;
}
