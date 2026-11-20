package com.daebangg.sayroam.notification.domain.error;

import com.daebangg.sayroam.global.exception.ErrorCode;
import com.daebangg.sayroam.global.exception.ErrorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    NOTIFICATION_NOT_FOUND(
            ErrorType.NOT_FOUND,
            "NOTIFICATION_NOT_FOUND",
            "Notification not found."
    ),
    NOTIFICATION_RECEIVER_INVALID(
            ErrorType.INVALID,
            "NOTIFICATION_RECEIVER_INVALID",
            "Notification receiver ID must be a positive number."
    ),
    NOTIFICATION_TYPE_REQUIRED(
            ErrorType.INVALID,
            "NOTIFICATION_TYPE_REQUIRED",
            "Notification type is required."
    ),
    NOTIFICATION_CONTENT_INVALID(
            ErrorType.INVALID,
            "NOTIFICATION_CONTENT_INVALID",
            "Notification content must not be blank and must be 255 characters or less."
    ),
    NOTIFICATION_EVENT_KEY_INVALID(
            ErrorType.INVALID,
            "NOTIFICATION_EVENT_KEY_INVALID",
            "Event key must not be blank, must have no leading or trailing whitespace, must be 150 characters or less, and must not use the reserved legacy:notification: prefix."
    ),
    NOTIFICATION_TARGET_INVALID(
            ErrorType.INVALID,
            "NOTIFICATION_TARGET_INVALID",
            "Target type and a positive target ID must either both be provided or both be omitted."
    ),
    NOTIFICATION_DELETED(
            ErrorType.CONFLICT,
            "NOTIFICATION_DELETED",
            "Deleted notifications cannot be modified."
    );

    private final ErrorType type;
    private final String code;
    private final String message;
}
