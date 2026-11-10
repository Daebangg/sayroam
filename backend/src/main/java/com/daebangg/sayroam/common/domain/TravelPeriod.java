package com.daebangg.sayroam.common.domain;

import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import jakarta.persistence.Embeddable;

import java.time.LocalDate;

// 여행 기간과 동행 희망 기간이 함께 쓴다
@Embeddable
public record TravelPeriod(
        LocalDate startDate,
        LocalDate endDate
) {

    public TravelPeriod {
        if (startDate == null || endDate == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START);
        }
    }

}
