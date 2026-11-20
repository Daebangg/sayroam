package com.daebangg.sayroam.notification.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

@Schema(requiredProperties = {"isRead"})
public record ReadAllNotificationsRequest(
        @NotNull @AssertTrue
        @Schema(description = "전체 읽음 처리에는 true만 허용", types = {"boolean"}, _const = "true", example = "true")
        Boolean isRead
) {
}
