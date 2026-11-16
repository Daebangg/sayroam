package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.domain.CompanionSearchStatus;
import com.daebangg.sayroam.user.domain.Gender;

import java.time.LocalDate;

public record CompanionSearchResult(
        Long id,
        Long destinationId,
        String legacyRegion,
        LocalDate startDate,
        LocalDate endDate,
        Gender preferredGender,
        Integer preferredMinAge,
        Integer preferredMaxAge,
        CompanionSearchStatus status
) {

    public static CompanionSearchResult from(CompanionSearch search) {
        AgeRange ageRange = search.getPreferredAgeRange();
        return new CompanionSearchResult(search.getId(), search.getDestinationId(), search.getRegion(),
                search.getPeriod().startDate(), search.getPeriod().endDate(), search.getPreferredGender(),
                ageRange == null ? null : ageRange.minAge(), ageRange == null ? null : ageRange.maxAge(),
                search.getStatus());
    }
}
