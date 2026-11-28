package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.common.domain.TravelPeriod;
import com.daebangg.sayroam.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompanionSearchValidationTest {
    private final TravelPeriod period = new TravelPeriod(LocalDate.of(2030, 4, 1), LocalDate.of(2030, 4, 3));

    @Test
    void validatesTextLengthsWithoutHttpAndCountsSupplementaryCharactersAsOne() {
        String title = "😀".repeat(CompanionSearch.MAX_TITLE_LENGTH);
        String introduction = "😀".repeat(CompanionSearch.MAX_INTRODUCTION_LENGTH);
        CompanionSearch search = CompanionSearch.builder().userId(1L).destinationId(1L).period(period)
                .title(" " + title + " ").introduction(" " + introduction + " ").build();
        assertThat(search.getTitle()).isEqualTo(title);
        assertThat(search.getIntroduction()).isEqualTo(introduction);
        long revision = search.getMatchingRevision();

        assertThatThrownBy(() -> CompanionSearch.builder().userId(1L).destinationId(1L).period(period)
                .title(title + "😀").build()).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> search.update(1L, period, null, null, "새 제목", introduction + "😀", false, false))
                .isInstanceOf(BusinessException.class);
        assertThat(search.getTitle()).isEqualTo(title);
        assertThat(search.getIntroduction()).isEqualTo(introduction);
        assertThat(search.getMatchingRevision()).isEqualTo(revision);
    }

    @Test
    void rejectsMissingOrNonPositiveIdentifiersWithoutDependingOnDatabaseConstraints() {
        assertThatThrownBy(() -> CompanionSearch.builder().destinationId(1L).period(period).build())
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CompanionSearch.builder().userId(0L).destinationId(1L).period(period).build())
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> CompanionSearch.builder().userId(1L).destinationId(-1L).period(period).build())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void invalidTravelStyleDoesNotReplaceTheSavedAnswersOrRevision() {
        CompanionSearch search = CompanionSearch.builder().userId(1L).destinationId(1L).period(period).build();
        Map<String, Integer> answers = Map.of("planning", 3, "daily-pace", 3, "time-together", 3, "spending", 3);
        search.updateTravelStyle(answers);
        long revision = search.getMatchingRevision();
        assertThatThrownBy(() -> search.updateTravelStyle(
                Map.of("planning", 6, "daily-pace", 3, "time-together", 3, "spending", 3)))
                .isInstanceOf(BusinessException.class);
        assertThat(search.travelStyleAnswers()).isEqualTo(answers);
        assertThat(search.getMatchingRevision()).isEqualTo(revision);
    }
}
