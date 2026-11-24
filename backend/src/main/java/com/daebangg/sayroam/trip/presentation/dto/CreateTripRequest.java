package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(requiredProperties = {"destinationId", "startDate", "endDate"})
public record CreateTripRequest(
        @Schema(description = "국내 여행지 목록(GET /api/destinations)에서 고른 여행지 ID", example = "3")
        @NotNull Long destinationId,
        @Schema(description = "여행 시작일", example = "2026-12-01")
        @NotNull LocalDate startDate,
        @Schema(description = "여행 종료일. 시작일과 같거나 이후", example = "2026-12-04")
        @NotNull LocalDate endDate,
        @Schema(description = "여행 이름. 생성 화면이 고른 목적지로 기본 이름을 만들어 보낸다. "
                + "앞뒤 공백을 지운 뒤 최대 30자(이모지도 한 글자). 생략하거나 비어 있으면 이름 없이 저장",
                example = "제주 여행", nullable = true)
        String title) {
}
