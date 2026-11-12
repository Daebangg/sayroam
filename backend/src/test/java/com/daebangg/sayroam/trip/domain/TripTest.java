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
        // when & then
        assertThatCode(() -> new Trip(USER_ID, PERIOD))
                .doesNotThrowAnyException();
    }

    @Test
    void 여행을_만든_회원인지_알_수_있다() {
        // given
        Trip trip = new Trip(USER_ID, PERIOD);

        // when & then
        assertThat(trip.isCreatedBy(USER_ID)).isTrue();
        assertThat(trip.isCreatedBy(OTHER_USER_ID)).isFalse();
    }

    @Test
    void 새로_만든_여행에는_채팅방을_연결할_수_있다() {
        // given
        Trip trip = new Trip(USER_ID, PERIOD);

        // when & then
        assertThatCode(trip::ensureRoomLinkable)
                .doesNotThrowAnyException();
    }

    @Test
    void 여행에_채팅방을_연결하면_연결된_채팅방_ID를_알_수_있다() {
        // given
        Trip trip = new Trip(USER_ID, PERIOD);

        // when
        trip.linkRoom(ROOM_ID);

        // then
        assertThat(trip.getRoomId()).isEqualTo(ROOM_ID);
    }

    @Test
    void 이미_채팅방이_연결되었으면_다른_채팅방을_연결할_수_없다() {
        // given
        Trip trip = new Trip(USER_ID, PERIOD);
        trip.linkRoom(ROOM_ID);

        // when & then
        assertThatThrownBy(() -> trip.linkRoom(OTHER_ROOM_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_ROOM_ALREADY_LINKED));
        assertThat(trip.getRoomId()).isEqualTo(ROOM_ID);
    }
}
