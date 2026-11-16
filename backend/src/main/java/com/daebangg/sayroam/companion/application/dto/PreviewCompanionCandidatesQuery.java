package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.user.domain.Gender;

import java.time.LocalDate;

public record PreviewCompanionCandidatesQuery(
        long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        Gender preferredGender,
        Integer preferredMinAge,
        Integer preferredMaxAge,
        String titleQuery
) {
}
