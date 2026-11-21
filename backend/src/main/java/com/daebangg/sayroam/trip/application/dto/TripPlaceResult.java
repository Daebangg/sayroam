package com.daebangg.sayroam.trip.application.dto;

import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceSource;

import java.math.BigDecimal;

// 여행 장소(일차·메모·추가한 회원)와 장소 정보(출처·이름·분류·주소·좌표)를 합친 조회 결과.
// 직접 추가한 장소(CUSTOM)는 category와 address가 null, 일차 미정이면 day가 null이다.
public record TripPlaceResult(Long tripPlaceId, PlaceSource source, String name, PlaceCategory category,
                              String address, BigDecimal latitude, BigDecimal longitude,
                              Integer day, String memo, Long addedById) {
}
