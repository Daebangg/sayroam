package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CompanionSearchMatchingTest {
    @Test
    void matchesThroughTheEndDateAndInvalidatesOldRevisionsWhenClosingAndReopening() {
        LocalDate end = LocalDate.of(2030, 4, 23);
        CompanionSearch search = CompanionSearch.builder().userId(1L).destinationId(1L)
                .period(new TravelPeriod(end.minusDays(2), end)).build();
        search.updateTravelStyle(Map.of("planning", 3, "daily-pace", 3, "time-together", 3, "spending", 3));
        long revision = search.getMatchingRevision();
        assertThat(search.canMatch(end)).isTrue();
        assertThat(search.canMatch(revision, end)).isTrue();
        assertThat(search.canMatch(end.plusDays(1))).isFalse();
        assertThat(search.canMatch(revision, end.plusDays(1))).isFalse();
        search.changeStatus(CompanionSearchStatus.CLOSED);
        assertThat(search.canMatch(end)).isFalse();
        assertThat(search.canMatch(revision, end)).isFalse();
        search.changeStatus(CompanionSearchStatus.ACTIVE);
        assertThat(search.canMatch(revision, end)).isFalse();
        assertThat(search.canMatch(end)).isTrue();
        assertThat(search.canMatch(search.getMatchingRevision(), end)).isTrue();
    }
}
