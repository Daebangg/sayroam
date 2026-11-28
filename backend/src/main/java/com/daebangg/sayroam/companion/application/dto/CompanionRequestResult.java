package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.TravelStyleMatch;
import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

public record CompanionRequestResult(long requestId, long searchId, long candidateSearchId,
                                     CompanionRequestStatus status, CompanionRequest.Direction direction,
                                     @Nullable Long roomId,
                                     @Nullable CandidateProfile candidate) {
    public static CompanionRequestResult from(CompanionRequest request, long userId, @Nullable MemberProfile profile,
                                               LocalDate today, CompanionRequestStatus status) {
        CompanionRequest.Direction direction = request.direction(userId);
        boolean sent = direction == CompanionRequest.Direction.SENT;
        CompanionSearch mine = sent ? request.getSenderSearch() : request.getReceiverSearch();
        CompanionSearch other = sent ? request.getReceiverSearch() : request.getSenderSearch();
        return new CompanionRequestResult(request.getId(), mine.getId(), other.getId(), status,
                direction, request.getRoomId(), profile == null ? null : CandidateProfile.from(mine, other, profile, today));
    }

    public record MemberProfile(long userId, String nickname, @Nullable String profileImageUrl,
                                @Nullable LocalDate birthDate, @Nullable Gender gender) {
    }

    public record CandidateProfile(@Nullable String title, @Nullable String introduction, String nickname,
                            @Nullable String profileImageUrl, LocalDate startDate, LocalDate endDate,
                            @Nullable Integer age, @Nullable Gender gender, @Nullable TravelStyleMatch travelStyleMatch) {
        static CandidateProfile from(CompanionSearch mine, CompanionSearch other, MemberProfile profile, LocalDate today) {
            Map<String, Integer> myAnswers = mine.travelStyleAnswers();
            Map<String, Integer> answers = other.travelStyleAnswers();
            TravelStyleMatch match = TravelStyleMatch.isComplete(myAnswers) && TravelStyleMatch.isComplete(answers)
                    ? TravelStyleMatch.compare(myAnswers, answers) : null;
            Integer age = other.isShowAge() && profile.birthDate() != null
                    ? Period.between(profile.birthDate(), today).getYears() : null;
            return new CandidateProfile(other.getTitle(), other.getIntroduction(), profile.nickname(), profile.profileImageUrl(),
                    other.getPeriod().startDate(), other.getPeriod().endDate(), age,
                    other.isShowGender() ? profile.gender() : null, match);
        }
    }
}
