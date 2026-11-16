package com.daebangg.sayroam.companion.application.port.out;

import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;

import java.time.LocalDate;
import java.util.List;

public interface CompanionCandidateQueryRepository {

    List<CompanionCandidateResult> findCandidates(CandidateQuery query);

    record CandidateQuery(long ownerId, long searchId, LocalDate ageReferenceDate, long offset, int limit) {
    }
}
