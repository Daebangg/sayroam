package com.daebangg.sayroam.notification.application.dto;

import com.daebangg.sayroam.notification.domain.Notification;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;

import java.time.LocalDateTime;

public record NotificationResult(
        Long id,
        NotificationType type,
        String content,
        NotificationTargetType targetType,
        Long targetId,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResult from(Notification notification) {
        return new NotificationResult(
                notification.getId(),
                notification.getType(),
                notification.getContent(),
                notification.getTargetType(),
                notification.getTargetId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
