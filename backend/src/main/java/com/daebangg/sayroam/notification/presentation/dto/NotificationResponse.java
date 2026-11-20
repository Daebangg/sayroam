package com.daebangg.sayroam.notification.presentation.dto;

import com.daebangg.sayroam.notification.application.dto.NotificationResult;
import com.daebangg.sayroam.notification.domain.NotificationTargetType;
import com.daebangg.sayroam.notification.domain.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneOffset;

@Schema(requiredProperties = {"id", "type", "content", "targetType", "targetId", "isRead", "createdAt"})
public record NotificationResponse(
        @Schema(description = "알림 ID", example = "42")
        Long id,
        @Schema(description = "알림 종류", enumAsRef = true)
        NotificationType type,
        @Schema(description = "알림 생성 시 저장한 문구", example = "여행에 초대되었습니다.")
        String content,
        @Schema(description = "연결 대상 종류. 연결 대상이 없으면 null", enumAsRef = true)
        @Nullable NotificationTargetType targetType,
        @Schema(description = "연결 대상 ID. 연결 대상이 없으면 null", types = {"integer", "null"}, format = "int64", example = "12")
        @Nullable Long targetId,
        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,
        @Schema(description = "알림 생성 시각. UTC 기준", example = "2026-11-20T09:00:00Z")
        Instant createdAt
) {
    public static NotificationResponse from(NotificationResult result) {
        return new NotificationResponse(result.id(), result.type(), result.content(),
                result.targetType(), result.targetId(), result.isRead(), result.createdAt().toInstant(ZoneOffset.UTC));
    }
}
