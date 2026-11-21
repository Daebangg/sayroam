package com.daebangg.sayroam.trip.presentation.dto;

import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceSource;
import com.daebangg.sayroam.trip.application.dto.TripPlaceResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(requiredProperties = {"places"})
public record TripPlaceListResponse(
        @Schema(description = "여행에 담긴 장소. 일차 순서대로, 일차 미정은 마지막, 같은 일차 안에서는 추가한 순서")
        List<TripPlaceResponse> places) {

    public static TripPlaceListResponse from(List<TripPlaceResult> results) {
        return new TripPlaceListResponse(results.stream()
                .map(TripPlaceResponse::from)
                .toList());
    }

    @Schema(name = "TripPlaceResponse", requiredProperties = {"tripPlaceId", "source", "name", "category", "address",
            "latitude", "longitude", "day", "memo", "addedById"})
    public record TripPlaceResponse(
            @Schema(description = "여행 장소 ID", example = "31")
            Long tripPlaceId,
            @Schema(description = "추가한 방법. TOUR는 장소 검색, CUSTOM은 지도에서 직접 추가", example = "TOUR")
            PlaceSource source,
            @Schema(description = "장소 이름", example = "송정해수욕장")
            String name,
            @Schema(description = "장소 분류. 직접 추가한 장소는 null", example = "ATTRACTION",
                    types = {"string", "null"})
            PlaceCategory category,
            @Schema(description = "주소. 직접 추가한 장소이거나 출처에 주소가 없으면 null",
                    example = "부산광역시 해운대구 송정해변로 62", types = {"string", "null"})
            String address,
            @Schema(description = "위도(WGS84)", example = "35.1786")
            BigDecimal latitude,
            @Schema(description = "경도(WGS84)", example = "129.1997")
            BigDecimal longitude,
            @Schema(description = "일차. 일차 미정이면 null", example = "1", types = {"integer", "null"})
            Integer day,
            @Schema(description = "메모. 없으면 null", example = "오전에 가기", types = {"string", "null"})
            String memo,
            @Schema(description = "장소를 추가한 회원 ID", example = "1")
            Long addedById) {

        static TripPlaceResponse from(TripPlaceResult result) {
            return new TripPlaceResponse(result.tripPlaceId(), result.source(), result.name(), result.category(),
                    result.address(), result.latitude(), result.longitude(), result.day(), result.memo(),
                    result.addedById());
        }
    }
}
