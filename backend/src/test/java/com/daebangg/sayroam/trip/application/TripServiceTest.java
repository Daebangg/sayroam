package com.daebangg.sayroam.trip.application;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.application.dto.TripSummaryResult;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import com.daebangg.sayroam.trip.infrastructure.persistence.TripJpaRepository;
import com.daebangg.sayroam.user.domain.Role;
import com.daebangg.sayroam.user.domain.User;
import com.daebangg.sayroam.user.infrastructure.persistence.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripServiceTest extends DatabaseIntegrationTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);
    private static final Long MISSING_TRIP_ID = 999_999L;

    @Autowired
    TripService tripService;

    @Autowired
    TripJpaRepository tripJpaRepository;

    @Autowired
    UserJpaRepository userRepository;

    @AfterEach
    void cleanUp() {
        tripJpaRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    void 여행을_만들면_저장된다() {
        // given
        Long userId = saveUser();

        // when
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // then
        assertThat(tripJpaRepository.findById(tripId)).isPresent();
        assertThat(tripJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 종료일이_시작일보다_앞서면_여행을_저장하지_않는다() {
        // given
        Long userId = saveUser();

        // when
        assertThatThrownBy(() -> tripService.create(userId, END_DATE, START_DATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TravelPeriodErrorCode.TRAVEL_PERIOD_END_BEFORE_START));

        // then
        assertThat(tripJpaRepository.count()).isZero();
    }

    @Test
    void 내가_만든_여행만_목록에_나온다() {
        // given
        Long userId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long myTripId = tripService.create(userId, START_DATE, END_DATE);
        tripService.create(otherId, START_DATE, END_DATE);

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(myTripId);
    }

    @Test
    void 목록은_나중에_만든_여행부터_나온다() {
        // given
        Long userId = saveUser();
        Long firstTripId = tripService.create(userId, START_DATE, END_DATE);
        Long secondTripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).extracting(TripSummaryResult::tripId).containsExactly(secondTripId, firstTripId);
    }

    @Test
    void 만든_여행이_없으면_빈_목록을_돌려준다() {
        // given
        Long userId = saveUser();

        // when
        List<TripSummaryResult> trips = tripService.findMine(userId);

        // then
        assertThat(trips).isEmpty();
    }

    @Test
    void 내가_만든_여행을_조회하면_기간과_비어_있는_제목과_채팅방_ID를_돌려준다() {
        // given
        Long userId = saveUser();
        Long tripId = tripService.create(userId, START_DATE, END_DATE);

        // when
        TripSummaryResult trip = tripService.find(userId, tripId);

        // then
        assertThat(trip.tripId()).isEqualTo(tripId);
        assertThat(trip.period()).isEqualTo(new TravelPeriod(START_DATE, END_DATE));
        assertThat(trip.title()).isNull();
        assertThat(trip.roomId()).isNull();
    }

    @Test
    void 다른_회원의_여행은_조회할_수_없다() {
        // given
        Long ownerId = saveUser();
        Long otherId = saveUser("다른 회원");
        Long tripId = tripService.create(ownerId, START_DATE, END_DATE);

        // when & then
        assertThatThrownBy(() -> tripService.find(otherId, tripId))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    @Test
    void 없는_여행은_조회할_수_없다() {
        // given
        Long userId = saveUser();

        // when & then
        assertThatThrownBy(() -> tripService.find(userId, MISSING_TRIP_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(TripErrorCode.TRIP_NOT_FOUND));
    }

    private Long saveUser() {
        return saveUser("여행자");
    }

    private Long saveUser(String nickname) {
        User user = User.builder().nickname(nickname).role(Role.USER).build();
        return userRepository.save(user).getId();
    }
}