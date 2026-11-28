package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.user.domain.Gender;

/** 회원·모집 잠금 전에 후보 배치를 찾기 위한 불변 조회 결과. 변경용 엔티티로 사용하지 않는다. */
public record CompanionMatchingSourceResult(long searchId, long userId, Long destinationId,
                                             TravelPeriod period, Gender preferredGender, AgeRange preferredAgeRange) {
}
