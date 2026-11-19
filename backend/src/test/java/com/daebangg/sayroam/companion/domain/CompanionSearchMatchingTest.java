package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class CompanionSearchMatchingTest {
    @Test
    void matchesThroughTheEndDateAndInvalidatesOldRevisionsWhenClosingAndReopening() {
        LocalDate end = LocalDate.of(2030, 4, 23);
        CompanionSearch search = CompanionSearch.builder().userId(1L).destinationId(1L)
                .period(new TravelPeriod(end.minusDays(2), end)).build();
        long revision = search.getMatchingRevision();
        assertThat(search.canMatch(revision, end)).isTrue();
        assertThat(search.canMatch(revision, end.plusDays(1))).isFalse();
        search.changeStatus(CompanionSearchStatus.CLOSED);
        assertThat(search.canMatch(revision, end)).isFalse();
        search.changeStatus(CompanionSearchStatus.ACTIVE);
        assertThat(search.canMatch(revision, end)).isFalse();
        assertThat(search.canMatch(search.getMatchingRevision(), end)).isTrue();
    }
}
