package com.daebangg.sayroam.companion.application.dto;

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
        String title
) {
    public record Page(List<CompanionCandidateResult> candidates, boolean hasNext) {
    }
}
