package com.daebangg.sayroam.place.application.dto;

import com.daebangg.sayroam.place.domain.PlaceCategory;

import java.math.BigDecimal;

/** 저장하지 않고 화면에 보여주는 외부 장소 검색 항목. address는 없을 수 있다. */
public record PlaceSearchItem(
        String sourceId,
        String name,
        PlaceCategory category,
        String address,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
