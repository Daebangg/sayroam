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

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(name = "trip")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Trip extends BaseTimeEntity {
    public static final int MAX_TITLE_LENGTH = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdById;

    // 여행 채팅. 여행을 만들 때 함께 만든다
    @Column(name = "room_id", nullable = false, unique = true)
    @Getter
    private Long roomId;

    // 국내 여행지 목록에서 고른 목적지. 지도 초기 위치에만 쓰며 담을 장소의 지역을 제한하지 않는다
    @Column(name = "destination_id", nullable = false)
    private Long destinationId;

    // 여행 이름. 생성 화면이 목적지로 기본 이름을 만들어 보낸다. 붙이지 않았으면 NULL
    @Column(length = MAX_TITLE_LENGTH)
    @Getter
    private String title;

    @Embedded
    @Getter
    private TravelPeriod period;

    // 친구 초대 링크. 아직 공유하지 않았으면 NULL
    @Embedded
    private TripInvitation invitation;

    public Trip(Long createdById, Long destinationId, TravelPeriod period, String title) {
        if (destinationId == null) {
            throw new BusinessException(TripErrorCode.TRIP_DESTINATION_REQUIRED);
        }
        if (period == null) {
            throw new BusinessException(TripErrorCode.TRIP_PERIOD_REQUIRED);
        }
        this.createdById = createdById;
        this.destinationId = destinationId;
        this.period = period;
        this.title = normalizeTitle(title);
    }

    public void rename(String title) {
        this.title = normalizeTitle(title);
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

    public String issueInvitation(LocalDateTime now) {
        if (!hasValidInvitation(now)) {
            invitation = TripInvitation.issue(now);
        }
        return invitation.getCode();
    }

    public void ensureInvitationValid(LocalDateTime now) {
        if (!hasValidInvitation(now)) {
            throw new BusinessException(TripErrorCode.TRIP_INVITATION_NOT_FOUND);
        }
    }

    public Optional<String> findValidInvitationCode(LocalDateTime now) {
        if (!hasValidInvitation(now)) {
            return Optional.empty();
        }
        return Optional.of(invitation.getCode());
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

    // 링크를 만든 적이 없거나 만료됐으면 쓸 수 있는 초대가 없다
    private boolean hasValidInvitation(LocalDateTime now) {
        return invitation != null && !invitation.isExpired(now);
    }
}
