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
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "companion_search")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CompanionSearch extends BaseTimeEntity {
    public static final int MAX_TITLE_LENGTH = 80;
    public static final int MAX_INTRODUCTION_LENGTH = 200;
    public static final int MAX_ACTIVE_SEARCHES = 3;

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

    @Getter
    @Column(length = MAX_TITLE_LENGTH)
    private String title;

    @Getter
    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Getter
    @Column(name = "show_age", nullable = false)
    private boolean showAge;

    @Getter
    @Column(name = "show_gender", nullable = false)
    private boolean showGender;

    @Getter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CompanionSearchStatus status;

    @Builder
    private CompanionSearch(Long userId, Long destinationId, TravelPeriod period, Gender preferredGender,
                            AgeRange preferredAgeRange, String title, @Nullable String introduction,
                            @Nullable Boolean showAge, @Nullable Boolean showGender) {
        this.userId = userId;
        this.status = CompanionSearchStatus.ACTIVE;
        update(destinationId, period, preferredGender, preferredAgeRange, title, introduction, showAge, showGender);
    }

    public void update(Long destinationId, TravelPeriod period, Gender preferredGender, AgeRange preferredAgeRange,
                       String title, @Nullable String introduction, @Nullable Boolean showAge,
                       @Nullable Boolean showGender) {
        if (status == CompanionSearchStatus.CLOSED) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_CLOSED);
        }
        if (destinationId == null || period == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        String normalizedTitle = normalizeText(title, MAX_TITLE_LENGTH);
        if (introduction != null) {
            this.introduction = normalizeText(introduction, MAX_INTRODUCTION_LENGTH);
        }
        if (showAge != null) {
            this.showAge = showAge;
        }
        if (showGender != null) {
            this.showGender = showGender;
        }
        this.title = normalizedTitle;
        this.destinationId = destinationId;
        this.region = null;
        this.period = period;
        this.preferredGender = preferredGender;
        this.preferredAgeRange = preferredAgeRange;
    }

    private String normalizeText(@Nullable String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.strip();
        if (normalized.codePointCount(0, normalized.length()) > maxLength) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    public static void validateActiveSearchCount(long activeCount) {
        if (activeCount >= MAX_ACTIVE_SEARCHES) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_LIMIT_EXCEEDED);
        }
    }

    public void changeStatus(CompanionSearchStatus status) {
        if (status == null) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT);
        }
        if (isReopening(status) && destinationId == null) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_DESTINATION_REQUIRED);
        }
        this.status = status;
    }

    public boolean isReopening(@Nullable CompanionSearchStatus targetStatus) {
        return status == CompanionSearchStatus.CLOSED && targetStatus == CompanionSearchStatus.ACTIVE;
    }

    public void validateCandidateSearch() {
        if (status == CompanionSearchStatus.CLOSED) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_CLOSED);
        }
        if (destinationId == null) {
            throw new BusinessException(CompanionSearchErrorCode.COMPANION_SEARCH_DESTINATION_REQUIRED);
        }
    }

    public CompanionRequest requestBy(Long senderId, String message) {
        return new CompanionRequest(senderId, userId, this, message);
    }

}
