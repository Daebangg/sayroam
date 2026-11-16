package com.daebangg.sayroam.place.presentation.dto;

import com.daebangg.sayroam.place.application.dto.PlaceSearchItem;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(requiredProperties = {"sourceId", "name", "category", "address", "latitude", "longitude"})
public record PlaceSearchItemResponse(
        @Schema(description = "TourAPI 장소 ID(contentid). 여행에 담을 때 사용", example = "126081")
        String sourceId,
        @Schema(description = "장소 이름", example = "송정해수욕장")
        String name,
        @Schema(description = "분류", example = "ATTRACTION")
        PlaceCategory category,
        @Schema(description = "주소. 출처에 없으면 null", example = "부산광역시 해운대구 송정해변로 62", types = {"string", "null"})
        String address,
        @Schema(description = "위도(WGS84)", example = "35.1786")
        BigDecimal latitude,
        @Schema(description = "경도(WGS84)", example = "129.1997")
        BigDecimal longitude) {

    public static PlaceSearchItemResponse from(PlaceSearchItem item) {
        return new PlaceSearchItemResponse(item.sourceId(), item.name(), item.category(), item.address(),
                item.latitude(), item.longitude());
    }
}
