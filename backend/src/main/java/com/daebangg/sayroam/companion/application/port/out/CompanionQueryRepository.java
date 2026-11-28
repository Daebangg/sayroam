package com.daebangg.sayroam.companion.application.port.out;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateMatchQuery;
import com.daebangg.sayroam.companion.application.dto.CompanionCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingCandidateResult;
import com.daebangg.sayroam.companion.application.dto.CompanionMatchingSourceResult;
import com.daebangg.sayroam.companion.application.dto.CompanionRequestResult;
import com.daebangg.sayroam.companion.application.dto.TravelStyleAnswerResult;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.user.domain.Gender;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CompanionQueryRepository {

    List<CompanionCandidateResult> findCandidates(CandidateQuery query);

    boolean isMatchingCandidate(CompanionCandidateMatchQuery query);

    List<TravelStyleAnswerResult> findTravelStyleAnswers(List<Long> userIds);

    List<CompanionRequestResult.MemberProfile> findMemberProfiles(List<Long> userIds);

    Optional<CompanionMatchingSourceResult> findMatchingSource(long searchId);

    List<CompanionMatchingCandidateResult> findMatchingCandidates(CompanionMatchingSourceResult source, LocalDate today,
                                                                long afterId, int limit);

    record CandidateQuery(Long searchId, long ownerId, long destinationId, TravelPeriod period, Gender preferredGender,
                          AgeRange preferredAgeRange, String titleQuery, LocalDate ageReferenceDate,
                          long offset, int limit, boolean styleSort, Map<String, Integer> travelStyle) {
    }
}
