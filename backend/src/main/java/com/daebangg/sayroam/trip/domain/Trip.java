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
    public static final int MAX_TITLE_LENGTH = 30;

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

    // 여행 이름. 생성 화면이 목적지로 기본 이름을 만들어 보낸다. 붙이지 않았으면 NULL
    @Column(length = MAX_TITLE_LENGTH)
    @Getter
    private String title;

    @Embedded
    @Getter
    private TravelPeriod period;

    public Trip(Long createdById, TravelPeriod period, String title) {
        if (period == null) {
            throw new BusinessException(TripErrorCode.TRIP_PERIOD_REQUIRED);
        }
        this.createdById = createdById;
        this.period = period;
        this.title = normalizeTitle(title);
    }

    public void rename(String title) {
        this.title = normalizeTitle(title);
    }

    public boolean isCreatedBy(Long userId) {
        return createdById.equals(userId);
    }

    public void ensurePlaceAddable(Integer day) {
        if (day != null && !period.containsDay(day)) {
            throw new BusinessException(TripErrorCode.TRIP_DAY_OUT_OF_RANGE);
        }
    }

    public void ensureRoomLinkable() {
        if (roomId != null) {
            throw new BusinessException(TripErrorCode.TRIP_ROOM_ALREADY_LINKED);
        }
    }

    public void linkRoom(Long roomId) {
        ensureRoomLinkable();
        this.roomId = roomId;
    }

    // 생성과 이름 변경이 함께 쓴다. 앞뒤 공백을 지우고 비면 이름 없음(null)으로 두며, 글자 수는 이모지도 한 글자로 센다
    private static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        String stripped = title.strip();
        if (stripped.codePointCount(0, stripped.length()) > MAX_TITLE_LENGTH) {
            throw new BusinessException(TripErrorCode.TRIP_TITLE_TOO_LONG);
        }
        return stripped;
    }
}
