package com.daebangg.sayroam.notification.application.dto;

import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;

public record CreateNotificationCommand(
        Long receiverId,
        NotificationType type,
        NotificationTargetType targetType,
        Long targetId,
        String eventKey,
        String content
) {
}
