package com.daebangg.sayroam.common.domain;

import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TravelPeriodTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);

    @Test
    void 종료일이_시작일보다_앞서면_기간을_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> new TravelPeriod(END_DATE, START_DATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));
    }

    @Test
    void 시작일이_없으면_기간을_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> new TravelPeriod(null, END_DATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 종료일이_없으면_기간을_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> new TravelPeriod(START_DATE, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 시작일과_종료일이_같으면_기간을_만들_수_있다() {
        // when & then
        assertThatCode(() -> new TravelPeriod(START_DATE, START_DATE))
                .doesNotThrowAnyException();
    }
}
