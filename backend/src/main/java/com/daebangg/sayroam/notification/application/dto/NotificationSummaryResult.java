package com.daebangg.sayroam.notification.application.dto;

import com.daebangg.sayroam.notification.domain.NotificationRepository;

public record NotificationSummaryResult(long totalCount, long unreadCount) {
    public static NotificationSummaryResult from(NotificationRepository.Counts counts) {
        return new NotificationSummaryResult(counts.totalCount(), counts.unreadCount());
    }
}
