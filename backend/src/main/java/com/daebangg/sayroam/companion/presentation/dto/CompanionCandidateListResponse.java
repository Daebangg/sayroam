package com.daebangg.sayroam.companion.presentation.dto;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.user.domain.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

@Schema(requiredProperties = {"candidates", "hasNext"})
public record CompanionCandidateListResponse(List<Candidate> candidates, boolean hasNext) {

    public static CompanionCandidateListResponse from(CompanionCandidateResult.Page page) {
        return new CompanionCandidateListResponse(page.candidates().stream().map(Candidate::from).toList(), page.hasNext());
    }

    @Schema(requiredProperties = {"searchId", "nickname", "profileImageUrl", "startDate", "endDate",
            "overlapStartDate", "overlapEndDate", "overlapDays", "title", "introduction", "age", "gender"})
    public record Candidate(
            long searchId,
            String nickname,
            @Schema(types = {"string", "null"}) String profileImageUrl,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate overlapStartDate,
            LocalDate overlapEndDate,
            int overlapDays,
            @Schema(types = {"string", "null"}) String title,
            @Schema(types = {"string", "null"}) @Nullable String introduction,
            @Schema(description = "후보가 공개한 만 나이. 비공개 또는 프로필 미입력 시 null",
                    types = {"integer", "null"}, format = "int32") @Nullable Integer age,
            @Schema(description = "후보가 공개한 성별. 비공개 또는 프로필 미입력 시 null",
                    enumAsRef = true) @Nullable Gender gender
    ) {
        private static Candidate from(CompanionCandidateResult result) {
            return new Candidate(result.searchId(), result.nickname(), result.profileImageUrl(),
                    result.startDate(), result.endDate(), result.overlapStartDate(), result.overlapEndDate(),
                    result.overlapDays(), result.title(), result.introduction(), result.age(), result.gender());
        }
    }
}
