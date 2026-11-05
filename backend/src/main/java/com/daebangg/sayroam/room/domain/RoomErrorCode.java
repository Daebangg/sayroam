package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {
    ROOM_NAME_TOO_LONG(ErrorType.INVALID, "ROOM_NAME_TOO_LONG", "Room name must be 20 characters or less");

    private final ErrorType type;
    private final String code;
    private final String message;
}
