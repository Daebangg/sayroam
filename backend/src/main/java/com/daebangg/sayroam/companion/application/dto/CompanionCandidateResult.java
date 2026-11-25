package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.domain.RecruitmentTravelStyle;
import com.daebangg.sayroam.companion.domain.TravelStyleMatch;
import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompanionCandidateResult(
        long searchId,
        String nickname,
        String profileImageUrl,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate overlapStartDate,
        LocalDate overlapEndDate,
        int overlapDays,
        String title,
        LocalDateTime updatedAt,
        @Nullable String introduction,
        @Nullable Integer age,
        @Nullable Gender gender,
        long userId,
        @Nullable RecruitmentTravelStyle travelStyle,
        @Nullable TravelStyleMatch travelStyleMatch,
        @Nullable Long requestId, @Nullable CompanionRequestStatus requestStatus,
        CompanionRequest.@Nullable Direction direction, boolean canRequest
) {
    public CompanionCandidateResult(long searchId, String nickname, String profileImageUrl, LocalDate startDate,
                                    LocalDate endDate, LocalDate overlapStartDate, LocalDate overlapEndDate,
                                    int overlapDays, String title, LocalDateTime updatedAt, String introduction, Integer age, Gender gender,
                                    long userId, RecruitmentTravelStyle travelStyle, Long requestId, CompanionRequestStatus status,
                                    Long senderId, long ownerId, boolean eligibleNow) {
        this(searchId, nickname, profileImageUrl, startDate, endDate, overlapStartDate, overlapEndDate,
                overlapDays, title, updatedAt, introduction, age, gender, userId, travelStyle, null, requestId,
                status == CompanionRequestStatus.PENDING && !eligibleNow ? CompanionRequestStatus.EXPIRED : status,
                requestDirection(senderId, ownerId), eligibleNow && requestId == null);
    }

    private static CompanionRequest.Direction requestDirection(Long senderId, long ownerId) {
        if (senderId == null) return null;
        return senderId == ownerId ? CompanionRequest.Direction.SENT : CompanionRequest.Direction.RECEIVED;
    }

    public CompanionCandidateResult withTravelStyleMatch(TravelStyleMatch match) {
        return new CompanionCandidateResult(searchId, nickname, profileImageUrl, startDate, endDate, overlapStartDate,
                overlapEndDate, overlapDays, title, updatedAt, introduction, age, gender, userId, travelStyle, match,
                requestId, requestStatus, direction, canRequest);
    }

    public record Page(List<CompanionCandidateResult> candidates, boolean hasNext) {
    }
}
