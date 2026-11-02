package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlace extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    // 일차 미배정은 NULL
    private Integer dayNumber;

    private Integer visitOrder;

    private String memo;

    @Column(name = "added_by", nullable = false)
    private Long addedById;

    @Builder
    private TripPlace(Trip trip, Long placeId, Integer dayNumber, Integer visitOrder, String memo, Long addedById) {
        this.trip = trip;
        this.placeId = placeId;
        this.dayNumber = dayNumber;
        this.visitOrder = visitOrder;
        this.memo = memo;
        this.addedById = addedById;
    }

}
