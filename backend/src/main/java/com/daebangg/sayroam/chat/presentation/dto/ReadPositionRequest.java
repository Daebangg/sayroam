package com.daebangg.sayroam.chat.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(requiredProperties = {"lastReadMessageId"})
public record ReadPositionRequest(
        @NotNull
        @Positive
        @Schema(description = "화면에서 마지막으로 본 메시지 ID. 지금 읽은 위치보다 앞선 값은 무시합니다", example = "120")
        Long lastReadMessageId
) {
}
