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

    @Column(name = "room_id", nullable = false, unique = true)
    private Long roomId;

    // 동행 매칭으로 만들어진 여행이면 그 요청, 친구끼리 만든 여행은 NULL
    @Column(name = "source_companion_request_id")
    private Long sourceCompanionRequestId;

    @Column(length = 50)
    private String region;

    // 날짜 미정이면 NULL
    @Embedded
    private TravelPeriod period;

    @Builder
    private Trip(Long roomId, Long sourceCompanionRequestId, String region, TravelPeriod period) {
        this.roomId = roomId;
        this.sourceCompanionRequestId = sourceCompanionRequestId;
        this.region = region;
        this.period = period;
    }

}
