package com.daebangg.sayroam.place.presentation.dto;

import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(requiredProperties = {"places", "hasNext"})
public record PlaceSearchResponse(
        @Schema(description = "검색된 장소. 축제·여행코스와 좌표가 없는 장소는 제외하므로 한 페이지가 20개보다 적을 수 있음")
        List<PlaceSearchItemResponse> places,
        @Schema(description = "다음 페이지가 있는지 여부", example = "true")
        boolean hasNext) {

    public static PlaceSearchResponse from(PlaceSearchResult result) {
        return new PlaceSearchResponse(result.places().stream()
                .map(PlaceSearchItemResponse::from)
                .toList(), result.hasNext());
    }
}
