package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.trip.application.dto.AddTripPlaceCommand;
import com.daebangg.sayroam.trip.domain.TripPlace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(requiredProperties = {"sourceId"})
public record AddTripPlaceRequest(
        @Schema(description = "TourAPI 장소 ID. 장소 검색 응답의 sourceId를 그대로 보냄", example = "126081")
        @NotBlank
        String sourceId,
        @Schema(description = "일차. 1부터 여행의 마지막 날까지. 생략하면 일차 미정", example = "2",
                minimum = "1", types = {"integer", "null"})
        @Min(1)
        Integer day,
        @Schema(description = "메모. 선택값이며 255자 이내", example = "오전에 가기",
                maxLength = TripPlace.MAX_MEMO_LENGTH, types = {"string", "null"})
        @Size(max = TripPlace.MAX_MEMO_LENGTH)
        String memo) {

    public AddTripPlaceCommand toCommand() {
        return new AddTripPlaceCommand(sourceId, day, memo);
    }
}
