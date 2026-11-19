package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.RecruitmentTravelStyle;
import com.daebangg.sayroam.companion.domain.TravelStyleMatch;
import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
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
        @Nullable String introduction,
        @Nullable Integer age,
        @Nullable Gender gender,
        long userId,
        @Nullable RecruitmentTravelStyle travelStyle,
        @Nullable TravelStyleMatch travelStyleMatch
) {
    public CompanionCandidateResult(long searchId, String nickname, String profileImageUrl, LocalDate startDate,
                                    LocalDate endDate, LocalDate overlapStartDate, LocalDate overlapEndDate,
                                    int overlapDays, String title, String introduction, Integer age, Gender gender,
                                    long userId, RecruitmentTravelStyle travelStyle) {
        this(searchId, nickname, profileImageUrl, startDate, endDate, overlapStartDate, overlapEndDate,
                overlapDays, title, introduction, age, gender, userId, travelStyle, null);
    }

    public CompanionCandidateResult withTravelStyleMatch(TravelStyleMatch match) {
        return new CompanionCandidateResult(searchId, nickname, profileImageUrl, startDate, endDate, overlapStartDate,
                overlapEndDate, overlapDays, title, introduction, age, gender, userId, travelStyle, match);
    }

    public record Page(List<CompanionCandidateResult> candidates, boolean hasNext) {
    }
}
