package com.daebangg.sayroam.place.application.dto;

import com.daebangg.sayroam.place.domain.PlaceCategory;

/** 장소 키워드 검색 조건. category가 null이면 모든 분류를 검색한다. page는 1부터 시작한다. */
public record PlaceSearchQuery(String keyword, PlaceCategory category, int page) {
}
