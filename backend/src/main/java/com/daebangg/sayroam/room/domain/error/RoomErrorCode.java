package com.daebangg.sayroam.room.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RoomErrorCode implements ErrorCode {
    ROOM_NAME_TOO_LONG(ErrorType.INVALID, "ROOM_NAME_TOO_LONG", "Room name must be 20 characters or less"),
    ROOM_ACCESS_DENIED(ErrorType.FORBIDDEN, "ROOM_ACCESS_DENIED", "Room access is denied"),
    ROOM_INVITATION_NOT_FOUND(ErrorType.NOT_FOUND, "ROOM_INVITATION_NOT_FOUND", "Room invitation does not exist"),
    ROOM_INVITATION_EXPIRED(ErrorType.EXPIRED, "ROOM_INVITATION_EXPIRED", "Room invitation has expired");

    private final ErrorType type;
    private final String code;
    private final String message;
}
