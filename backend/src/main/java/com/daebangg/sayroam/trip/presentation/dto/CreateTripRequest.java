package com.daebangg.sayroam.trip.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(requiredProperties = {"startDate", "endDate"})
public record CreateTripRequest(
        @Schema(description = "여행 시작일", example = "2026-12-01")
        @NotNull LocalDate startDate,
        @Schema(description = "여행 종료일. 시작일과 같거나 이후", example = "2026-12-04")
        @NotNull LocalDate endDate) {
}
