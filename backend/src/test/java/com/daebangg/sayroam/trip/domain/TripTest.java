package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


class TripTest {
    private static final Long USER_ID = 1L;

    @Test
    void 기간이_없으면_여행을_만들_수_없다() {
        // given
        TravelPeriod noPeriod = null;

        // when & then
        assertThatThrownBy(() -> new Trip(USER_ID, noPeriod))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_PERIOD_REQUIRED));
    }

    @Test
    void 기간이_있으면_여행을_만들_수_있다() {
        // given
        TravelPeriod period = new TravelPeriod(LocalDate.of(2026, 12, 1),
                LocalDate.of(2026, 12, 4));

        // when & then
        assertThatCode(() -> new Trip(USER_ID, period))
                .doesNotThrowAnyException();
    }

}