package com.daebangg.sayroam.trip.domain;

import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.trip.domain.error.TripErrorCode;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "trip_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TripPlace extends BaseTimeEntity {
    public static final int MAX_MEMO_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(name = "place_id", nullable = false)
    private Long placeId;

    @Column(name = "added_by", nullable = false)
    private Long addedById;

    // 일차 미배정은 NULL
    private Integer dayNumber;

    private String memo;

    public TripPlace(Trip trip, Long placeId, Long addedById, Integer dayNumber, String memo) {
        trip.ensurePlaceAddable(dayNumber);
        if (memo != null && memo.length() > MAX_MEMO_LENGTH) {
            throw new BusinessException(TripErrorCode.TRIP_PLACE_MEMO_TOO_LONG);
        }
        this.trip = trip;
        this.placeId = placeId;
        this.addedById = addedById;
        this.dayNumber = dayNumber;
        this.memo = memo;
    }

}
