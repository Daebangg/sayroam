package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(requiredProperties = {"tripId", "title", "startDate", "endDate", "roomId",
        "destinationId", "destinationName", "boundsSouth", "boundsWest", "boundsNorth", "boundsEast"})
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
        Long roomId,
        @Schema(description = "여행을 만들 때 고른 여행지 ID. 지도 초기 위치에만 쓰며 담을 장소의 지역을 제한하지 않는다",
                example = "2")
        Long destinationId,
        @Schema(description = "여행지 이름", example = "부산")
        String destinationName,
        @Schema(description = "지도에 여행지 전체를 맞춰 보여 줄 영역의 남쪽 끝 위도. 먼 섬은 제외",
                example = "34.932748")
        BigDecimal boundsSouth,
        @Schema(description = "영역의 서쪽 끝 경도", example = "128.761718")
        BigDecimal boundsWest,
        @Schema(description = "영역의 북쪽 끝 위도", example = "35.389041")
        BigDecimal boundsNorth,
        @Schema(description = "영역의 동쪽 끝 경도", example = "129.305605")
        BigDecimal boundsEast) {

    public static TripSummaryResponse from(TripSummaryResult result) {
        return new TripSummaryResponse(result.tripId(), result.title(),
                result.period().startDate(), result.period().endDate(), result.roomId(),
                result.destinationId(), result.destinationName(),
                result.boundsSouth(), result.boundsWest(), result.boundsNorth(), result.boundsEast());
    }
}
