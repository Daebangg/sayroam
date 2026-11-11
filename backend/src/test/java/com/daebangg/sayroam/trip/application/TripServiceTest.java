package com.daebangg.sayroam.trip.application;

import com.daebangg.sayroam.common.domain.error.TravelPeriodErrorCode;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import com.daebangg.sayroam.trip.application.service.TripService;
import com.daebangg.sayroam.trip.infrastructure.persistence.TripJpaRepository;
import com.daebangg.sayroam.user.domain.entity.User;
import com.daebangg.sayroam.user.domain.repository.UserRepository;
import com.daebangg.sayroam.user.domain.type.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripServiceTest extends DatabaseIntegrationTest {
    private static final LocalDate START_DATE = LocalDate.of(2026, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 4);

    @Autowired
    TripService tripService;

    @Autowired
    TripJpaRepository tripJpaRepository;

    @Autowired
    UserRepository userRepository;

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

    private Long saveUser() {
        User user = User.builder().nickname("여행자").role(Role.USER).build();
        return userRepository.save(user).getId();
    }
}