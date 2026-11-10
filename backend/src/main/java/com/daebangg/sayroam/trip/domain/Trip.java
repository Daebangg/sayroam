package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

// 여행 상태(계획 중/여행 중/종료)는 저장하지 않고 여행 기간과 오늘 날짜로 계산한다
@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 여행을 만든 회원. 방이 없는 동안에는 이 회원만 수정한다
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdById;

    // 혼자 가는 여행은 NULL
    @Column(name = "room_id", unique = true)
    private Long roomId;

    @Column(length = 50)
    private String region;

    // 날짜 미정이면 NULL
    @Embedded
    private TravelPeriod period;

    @Builder
    private Trip(Long createdById, Long roomId, String region, TravelPeriod period) {
        this.createdById = createdById;
        this.roomId = roomId;
        this.region = region;
        this.period = period;
    }

}
