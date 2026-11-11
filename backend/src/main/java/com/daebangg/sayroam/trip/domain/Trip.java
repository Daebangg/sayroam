package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    // 여행을 만든 회원. 방이 없는 동안에는 이 회원만 수정한다
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdById;

    // 혼자 가는 여행은 NULL
    @Column(name = "room_id", unique = true)
    @Getter
    private Long roomId;

    // 회원이 직접 붙인 이름. 붙이지 않았으면 NULL
    @Column(length = 20)
    @Getter
    private String title;

    @Embedded
    @Getter
    private TravelPeriod period;

    public Trip(Long createdById, TravelPeriod period) {
        if (period == null) {
            throw new BusinessException(TripErrorCode.TRIP_PERIOD_REQUIRED);
        }
        this.createdById = createdById;
        this.period = period;
    }

    public boolean isCreatedBy(Long userId) {
        return createdById.equals(userId);
    }
}
