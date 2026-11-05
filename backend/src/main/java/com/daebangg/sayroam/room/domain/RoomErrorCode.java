package com.daebangg.sayroam.room.domain;

import com.daebangg.sayroam.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {
    ROOM_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "ROOM_NAME_TOO_LONG", "Room name must be 20 characters or less");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
