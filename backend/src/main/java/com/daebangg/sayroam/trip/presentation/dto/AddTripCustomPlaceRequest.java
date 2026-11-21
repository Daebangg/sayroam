package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.place.domain.PlaceName;
import com.daebangg.sayroam.trip.application.dto.AddTripCustomPlaceCommand;
import com.daebangg.sayroam.trip.domain.TripPlace;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(requiredProperties = {"name", "latitude", "longitude"})
public record AddTripCustomPlaceRequest(
        @Schema(description = "회원이 직접 입력한 장소 이름. 100자 이내", example = "숙소 (에어비앤비)",
                maxLength = PlaceName.MAX_LENGTH)
        @NotBlank
        @Size(min = 1, max = PlaceName.MAX_LENGTH)
        String name,
        @Schema(description = "지도에서 고른 위치의 위도(WGS84)", example = "33.4996213", minimum = "-90", maximum = "90")
        @NotNull
        @DecimalMin("-90")
        @DecimalMax("90")
        BigDecimal latitude,
        @Schema(description = "지도에서 고른 위치의 경도(WGS84)", example = "126.5311884", minimum = "-180", maximum = "180")
        @NotNull
        @DecimalMin("-180")
        @DecimalMax("180")
        BigDecimal longitude,
        @Schema(description = "일차. 1부터 여행의 마지막 날까지. 생략하면 일차 미정", example = "2",
                minimum = "1", types = {"integer", "null"})
        @Min(1)
        Integer day,
        @Schema(description = "메모. 선택값이며 255자 이내", example = "체크인 4시",
                maxLength = TripPlace.MAX_MEMO_LENGTH, types = {"string", "null"})
        @Size(max = TripPlace.MAX_MEMO_LENGTH)
        String memo) {

    public AddTripCustomPlaceCommand toCommand() {
        return new AddTripCustomPlaceCommand(name, latitude, longitude, day, memo);
    }
}
