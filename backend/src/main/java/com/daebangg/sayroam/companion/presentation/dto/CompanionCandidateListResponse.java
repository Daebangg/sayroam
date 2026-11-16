package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(requiredProperties = {"candidates", "hasNext"})
public record CompanionCandidateListResponse(List<Candidate> candidates, boolean hasNext) {

    public static CompanionCandidateListResponse from(CompanionCandidateResult.Page page) {
        return new CompanionCandidateListResponse(page.candidates().stream().map(Candidate::from).toList(), page.hasNext());
    }

    @Schema(requiredProperties = {"searchId", "nickname", "profileImageUrl", "startDate", "endDate",
            "overlapStartDate", "overlapEndDate", "overlapDays"})
    public record Candidate(
            long searchId,
            String nickname,
            @Schema(types = {"string", "null"}) String profileImageUrl,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate overlapStartDate,
            LocalDate overlapEndDate,
            int overlapDays
    ) {
        private static Candidate from(CompanionCandidateResult result) {
            return new Candidate(result.searchId(), result.nickname(), result.profileImageUrl(),
                    result.startDate(), result.endDate(), result.overlapStartDate(), result.overlapEndDate(),
                    result.overlapDays());
        }
    }
}
