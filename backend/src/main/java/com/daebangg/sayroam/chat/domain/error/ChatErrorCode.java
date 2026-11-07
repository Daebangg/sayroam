package com.daebangg.sayroam.chat.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ChatErrorCode implements ErrorCode {
    CHAT_CONTENT_BLANK(ErrorType.INVALID, "CHAT_CONTENT_BLANK", "Message content must not be blank"),
    CHAT_CONTENT_TOO_LONG(ErrorType.INVALID, "CHAT_CONTENT_TOO_LONG", "Message content must be 500 characters or less"),
    CHAT_CLIENT_MESSAGE_ID_INVALID(ErrorType.INVALID, "CHAT_CLIENT_MESSAGE_ID_INVALID", "Client message ID must be a valid UUID");

    private final ErrorType type;
    private final String code;
    private final String message;
}
