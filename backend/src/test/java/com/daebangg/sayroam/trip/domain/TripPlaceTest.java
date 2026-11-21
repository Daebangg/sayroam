package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripPlaceTest {
    private static final Long USER_ID = 1L;
    private static final Long PLACE_ID = 10L;
    private static final Integer DAY = 1;
    private static final Trip TRIP = new Trip(USER_ID, new TravelPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4)), "제주 여행");

    @Test
    void 메모_없이_여행_장소를_만들_수_있다() {
        // when & then
        assertThatCode(() -> new TripPlace(TRIP, PLACE_ID, USER_ID, DAY, null))
                .doesNotThrowAnyException();
    }

    @Test
    void 메모가_255자면_여행_장소를_만들_수_있다() {
        // given
        String memo = "가".repeat(TripPlace.MAX_MEMO_LENGTH);

        // when & then
        assertThatCode(() -> new TripPlace(TRIP, PLACE_ID, USER_ID, DAY, memo))
                .doesNotThrowAnyException();
    }

    @Test
    void 메모가_255자를_넘으면_여행_장소를_만들_수_없다() {
        // given
        String memo = "가".repeat(TripPlace.MAX_MEMO_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> new TripPlace(TRIP, PLACE_ID, USER_ID, DAY, memo))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_PLACE_MEMO_TOO_LONG));
    }

    @Test
    void 일차_미정으로_여행_장소를_만들_수_있다() {
        // when & then
        assertThatCode(() -> new TripPlace(TRIP, PLACE_ID, USER_ID, null, null))
                .doesNotThrowAnyException();
    }

    @Test
    void 여행_기간을_벗어난_일차로는_여행_장소를_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> new TripPlace(TRIP, PLACE_ID, USER_ID, 5, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));
    }
}