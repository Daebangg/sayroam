package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.domain.error.CompanionSearchErrorCode;
import com.daebangg.sayroam.global.entity.BaseTimeEntity;
import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import com.daebangg.sayroam.user.domain.Gender;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "companion_search")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionSearch extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Getter
    @Column(name = "destination_id")
    private Long destinationId;

    @Getter
    @Column(length = 50)
    private String region;

    @Getter
    @Embedded
    @AttributeOverride(name = "startDate", column = @Column(name = "start_date", nullable = false))
    @AttributeOverride(name = "endDate", column = @Column(name = "end_date", nullable = false))
    private TravelPeriod period;

    // NULL이면 무관
    @Getter
    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender preferredGender;

    // NULL이면 나이 제한 없음
    @Getter
    @Embedded
    @AttributeOverride(name = "minAge", column = @Column(name = "preferred_min_age"))
    @AttributeOverride(name = "maxAge", column = @Column(name = "preferred_max_age"))
    private AgeRange preferredAgeRange;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionSearchStatus status;

    @Builder
    private CompanionSearch(Long userId, Long destinationId, TravelPeriod period, Gender preferredGender,
                          AgeRange preferredAgeRange, String introduction) {
        this.userId = userId;
        this.introduction = introduction;
        this.status = CompanionSearchStatus.ACTIVE;
        update(destinationId, period, preferredGender, preferredAgeRange);
    }

    public void update(Long destinationId, TravelPeriod period, Gender preferredGender, AgeRange preferredAgeRange) {
        if (status == CompanionSearchStatus.CLOSED) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_CLOSED);
        }
        if (destinationId == null || period == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        this.destinationId = destinationId;
        this.region = null;
        this.period = period;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = preferredAgeRange;
    }

    public void changeStatus(CompanionSearchStatus status) {
        if (status != CompanionSearchStatus.CLOSED) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        this.status = status;
    }

    public CompanionRequest requestBy(Long senderId, String message) {
        return new CompanionRequest(senderId, userId, this, message);
    }

}
