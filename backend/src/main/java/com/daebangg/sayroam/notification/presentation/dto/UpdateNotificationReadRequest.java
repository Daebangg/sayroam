package com.daebangg.sayroam.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(requiredProperties = {"isRead"})
public record UpdateNotificationReadRequest(
        @NotNull
        @Schema(description = "true는 읽음 처리, false는 읽지 않음 처리", example = "true")
        Boolean isRead
) {
}
