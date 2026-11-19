package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Map;

public record PreviewCompanionCandidatesQuery(
        long destinationId,
        LocalDate startDate,
        LocalDate endDate,
        Gender preferredGender,
        Integer preferredMinAge,
        Integer preferredMaxAge,
        String titleQuery,
        @Nullable Map<String, Integer> travelStyle
) {
}
