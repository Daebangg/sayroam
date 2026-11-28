package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.companion.domain.CompanionCandidateSort;

public record FindCompanionCandidatesQuery(
        String titleQuery,
        int page,
        int size,
        CompanionCandidateSort sort
) {
}
