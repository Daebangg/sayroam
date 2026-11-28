package com.daebangg.sayroam.companion.application.dto;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.companion.domain.AgeRange;
import com.daebangg.sayroam.companion.domain.CompanionRequest;
import com.daebangg.sayroam.companion.domain.CompanionSearch;
import com.daebangg.sayroam.user.domain.Gender;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

public record CompanionCandidateMatchQuery(
        long ownerId,
        long destinationId,
        TravelPeriod period,
        @Nullable Gender preferredGender,
        @Nullable AgeRange preferredAgeRange,
        LocalDate today,
        long candidateSearchId
) {
    public static CompanionCandidateMatchQuery from(CompanionSearch source, LocalDate today, long candidateSearchId) {
        return new CompanionCandidateMatchQuery(source.getUserId(), source.getDestinationId(), source.getPeriod(),
                source.getPreferredGender(), source.getPreferredAgeRange(), today, candidateSearchId);
    }

    public static CompanionCandidateMatchQuery from(CompanionRequest request, LocalDate today) {
        return from(request.getSenderSearch(), today, request.getReceiverSearch().getId());
    }
}
