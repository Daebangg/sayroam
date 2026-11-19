package com.daebangg.sayroam.companion.application.port.out;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.companion.application.dto.TravelStyleAnswerResult;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.user.domain.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CompanionCandidateQueryRepository {

    List<CompanionCandidateResult> findCandidates(CandidateQuery query);

    List<CompanionMatchingCandidateResult> findMatchingCandidates(CompanionSearch source, LocalDate today,
                                                                long afterId, int limit);

    List<TravelStyleAnswerResult> findTravelStyleAnswers(List<Long> userIds);

    record CandidateQuery(long ownerId, long destinationId, TravelPeriod period, Gender preferredGender,
                          AgeRange preferredAgeRange, String titleQuery, LocalDate ageReferenceDate,
                          long offset, int limit, boolean styleSort, Map<String, Integer> travelStyle) {
    }
}
