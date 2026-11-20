package com.daebangg.sayroam.notification.presentation.dto;

import com.daebangg.sayroam.notification.application.dto.NotificationSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(requiredProperties = {"totalCount", "unreadCount"})
public record NotificationSummaryResponse(
        @Schema(description = "삭제되지 않은 본인 알림의 전체 개수", types = {"integer"}, format = "int64", minimum = "0", example = "12")
        long totalCount,
        @Schema(description = "삭제되지 않은 본인 알림 중 읽지 않은 개수", types = {"integer"}, format = "int64", minimum = "0", example = "3")
        long unreadCount
) {
    public static NotificationSummaryResponse from(NotificationSummaryResult result) {
        return new NotificationSummaryResponse(result.totalCount(), result.unreadCount());
    }
}
