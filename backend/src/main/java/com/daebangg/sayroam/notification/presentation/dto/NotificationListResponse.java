package com.daebangg.sayroam.notification.presentation.dto;

import com.daebangg.sayroam.notification.application.dto.NotificationResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"notifications"})
public record NotificationListResponse(
        @Schema(description = "본인의 삭제되지 않은 알림. ID 내림차순으로 정렬")
        List<NotificationResponse> notifications
) {
    public static NotificationListResponse from(List<NotificationResult> results) {
        return new NotificationListResponse(results.stream().map(NotificationResponse::from).toList());
    }
}
