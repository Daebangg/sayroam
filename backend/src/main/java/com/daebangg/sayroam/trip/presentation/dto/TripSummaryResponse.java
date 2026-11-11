package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(requiredProperties = {"tripId", "title", "startDate", "endDate", "roomId"})
public record TripSummaryResponse(
        @Schema(description = "여행 ID", example = "12")
        Long tripId,
        @Schema(description = "회원이 붙인 제목. 붙이지 않았으면 null", example = "제주 3박 4일", types = {"string", "null"})
        String title,
        @Schema(description = "여행 시작일", example = "2026-12-01")
        LocalDate startDate,
        @Schema(description = "여행 종료일", example = "2026-12-04")
        LocalDate endDate,
        @Schema(description = "연결된 채팅방 ID. 혼자 가는 여행이면 null", example = "7", types = {"integer", "null"})
        Long roomId) {

    public static TripSummaryResponse from(TripSummaryResult result) {
        return new TripSummaryResponse(result.tripId(), result.title(),
                result.period().startDate(), result.period().endDate(), result.roomId());
    }
}
