package com.daebangg.sayroam.place.infrastructure.client;

import com.daebangg.sayroam.place.domain.PlaceCategory;

import java.util.Arrays;
import java.util.Optional;

// TourAPI 관광타입(contentTypeId)과 서비스 분류의 대응. 축제공연행사(15)와 여행코스(25)는 장소로 다루지 않아 대응이 없다.
enum TourContentType {
    ATTRACTION("12", PlaceCategory.ATTRACTION),
    CULTURE("14", PlaceCategory.CULTURE),
    ACTIVITY("28", PlaceCategory.ACTIVITY),
    LODGING("32", PlaceCategory.LODGING),
    SHOPPING("38", PlaceCategory.SHOPPING),
    RESTAURANT("39", PlaceCategory.RESTAURANT);

    private final String code;
    private final PlaceCategory category;

    TourContentType(String code, PlaceCategory category) {
        this.code = code;
        this.category = category;
    }

    static Optional<PlaceCategory> categoryOf(String code) {
        return Arrays.stream(values())
                .filter(type -> type.code.equals(code))
                .map(type -> type.category)
                .findFirst();
    }

    static String codeOf(PlaceCategory category) {
        return Arrays.stream(values())
                .filter(type -> type.category == category)
                .map(type -> type.code)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No TourAPI content type for " + category));
    }

}
