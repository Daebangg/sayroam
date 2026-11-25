package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionRequestResult;
import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionRequestStatus;
import com.daebangg.sayroam.user.domain.Gender;
import com.daebangg.sayroam.companion.presentation.dto.CompanionCandidateListResponse.TravelStyleMatchResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

@Schema(requiredProperties = {"requestId", "searchId", "candidateSearchId", "status", "direction", "candidate"})
public record CompanionRequestResponse(long requestId, long searchId, long candidateSearchId,
                                       CompanionRequestStatus status, CompanionRequest.Direction direction,
                                       @Nullable RequestCandidate candidate) {
    public static CompanionRequestResponse from(CompanionRequestResult result) {
        return new CompanionRequestResponse(result.requestId(), result.searchId(), result.candidateSearchId(),
                result.status(), result.direction(), result.candidate() == null ? null : RequestCandidate.from(result.candidate()));
    }

    @Schema(requiredProperties = {"title", "introduction", "nickname", "profileImageUrl", "startDate", "endDate",
            "age", "gender", "travelStyleMatch"})
    public record RequestCandidate(@Nullable String title, @Nullable String introduction, String nickname,
                                   @Nullable String profileImageUrl, LocalDate startDate, LocalDate endDate,
                                   @Nullable Integer age, @Nullable Gender gender,
                                   @Nullable TravelStyleMatchResponse travelStyleMatch) {
        private static RequestCandidate from(CompanionRequestResult.CandidateProfile profile) {
            return new RequestCandidate(profile.title(), profile.introduction(), profile.nickname(),
                    profile.profileImageUrl(), profile.startDate(), profile.endDate(), profile.age(), profile.gender(),
                    profile.travelStyleMatch() == null ? null : TravelStyleMatchResponse.from(profile.travelStyleMatch()));
        }
    }
}
