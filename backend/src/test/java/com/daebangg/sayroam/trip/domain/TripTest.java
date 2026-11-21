package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripTest {
    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long ROOM_ID = 3L;
    private static final Long OTHER_ROOM_ID = 99L;
    private static final TravelPeriod PERIOD =
            new TravelPeriod(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 4));
    private static final String TITLE = "제주 여행";

    @Test
    void 기간이_없으면_여행을_만들_수_없다() {
        // given
        TravelPeriod noPeriod = null;

        // when & then
        assertThatThrownBy(() -> new Trip(USER_ID, noPeriod, TITLE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_PERIOD_REQUIRED));
    }

    @Test
    void 기간이_있으면_여행을_만들_수_있다() {
        // when & then
        assertThatCode(() -> new Trip(USER_ID, PERIOD, TITLE))
                .doesNotThrowAnyException();
    }

    @Test
    void 제목의_앞뒤_공백을_지워_저장한다() {
        // when
        Trip trip = new Trip(USER_ID, PERIOD, "  제주 여행 ");

        // then
        assertThat(trip.getTitle()).isEqualTo("제주 여행");
    }

    @Test
    void 제목_없이_만든_여행은_제목이_없다() {
        // when
        Trip trip = new Trip(USER_ID, PERIOD, null);

        // then
        assertThat(trip.getTitle()).isNull();
    }

    @Test
    void 공백뿐인_제목은_제목_없음으로_저장한다() {
        // when
        Trip trip = new Trip(USER_ID, PERIOD, "   ");

        // then
        assertThat(trip.getTitle()).isNull();
    }

    @Test
    void 제목이_30자를_넘으면_여행을_만들지_않는다() {
        // given
        String title = "가".repeat(31);

        // when & then
        assertThatThrownBy(() -> new Trip(USER_ID, PERIOD, title))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_TITLE_TOO_LONG));
    }

    @Test
    void 이모지를_한_글자로_세어_30자_제목까지_허용한다() {
        // given
        String title = "🍊".repeat(30);

        // when
        Trip trip = new Trip(USER_ID, PERIOD, title);

        // then
        assertThat(trip.getTitle()).isEqualTo(title);
    }

    @Test
    void 앞뒤_공백을_지운_길이로_30자를_판단한다() {
        // given
        String title = " " + "가".repeat(30) + " ";

        // when
        Trip trip = new Trip(USER_ID, PERIOD, title);

        // then
        assertThat(trip.getTitle()).isEqualTo("가".repeat(30));
    }

    @Test
    void 여행을_만든_회원인지_알_수_있다() {
        // given
        Trip trip = newTrip();

        // when & then
        assertThat(trip.isCreatedBy(USER_ID)).isTrue();
        assertThat(trip.isCreatedBy(OTHER_USER_ID)).isFalse();
    }

    @Test
    void 새로_만든_여행에는_채팅방을_연결할_수_있다() {
        // given
        Trip trip = newTrip();

        // when & then
        assertThatCode(trip::ensureRoomLinkable)
                .doesNotThrowAnyException();
    }

    @Test
    void 여행에_채팅방을_연결하면_연결된_채팅방_ID를_알_수_있다() {
        // given
        Trip trip = newTrip();

        // when
        trip.linkRoom(ROOM_ID);

        // then
        assertThat(trip.getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void 이미_채팅방이_연결되었으면_다른_채팅방을_연결할_수_없다() {
        // given
        Trip trip = newTrip();
        trip.linkRoom(ROOM_ID);

        // when & then
        assertThatThrownBy(() -> trip.linkRoom(OTHER_ROOM_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_ROOM_ALREADY_LINKED));
        assertThat(trip.getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void 기간_안의_일차면_장소를_추가할_수_있다() {
        // given
        Trip trip = newTrip();

        // when & then
        assertThatCode(() -> trip.ensurePlaceAddable(4))
                .doesNotThrowAnyException();
    }

    @Test
    void 일차_미정이면_장소를_추가할_수_있다() {
        // given
        Trip trip = newTrip();

        // when & then
        assertThatCode(() -> trip.ensurePlaceAddable(null))
                .doesNotThrowAnyException();
    }

    @Test
    void 일차가_기간을_벗어나면_장소를_추가할_수_없다() {
        // given
        Trip trip = newTrip();

        // when & then
        assertThatThrownBy(() -> trip.ensurePlaceAddable(5))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_DAY_OUT_OF_RANGE));
    }

    private Trip newTrip() {
        return new Trip(USER_ID, PERIOD, TITLE);
    }
}
