package com.daebangg.sayroam.companion.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TravelStyleMatchTest {
    @Test
    void comparesAllFourScoresAndClassifiesIndividualDifferences() {
        TravelStyleMatch match = TravelStyleMatch.compare(answers(2, 4, 2, 5), answers(2, 5, 4, 2));
        assertThat(match.score()).isEqualTo(62.5);
        assertThat(match.category()).isEqualTo(TravelStyleMatch.Category.NEEDS_DISCUSSION);
        assertThat(match.items()).extracting(TravelStyleMatch.Item::difference).containsExactly(0, 1, 2, 3);
        assertThat(match.items()).extracting(TravelStyleMatch.Item::similarity).containsExactly(
                TravelStyleMatch.Similarity.SAME, TravelStyleMatch.Similarity.SIMILAR,
                TravelStyleMatch.Similarity.SOMEWHAT_DIFFERENT, TravelStyleMatch.Similarity.DIFFERENT);
    }

    @Test
    void classifiesByTheLargestDifferenceEvenWhenTotalScoresTie() {
        Map<String, Integer> mine = answers(2, 4, 2, 5);
        TravelStyleMatch similar = TravelStyleMatch.compare(mine, answers(2, 5, 2, 4));
        TravelStyleMatch different = TravelStyleMatch.compare(mine, answers(2, 4, 4, 5));
        assertThat(similar.score()).isEqualTo(different.score()).isEqualTo(87.5);
        assertThat(similar.category()).isEqualTo(TravelStyleMatch.Category.SIMILAR);
        assertThat(different.category()).isEqualTo(TravelStyleMatch.Category.NEEDS_DISCUSSION);
        assertThat(TravelStyleMatch.compare(answers(1, 1, 1, 1), answers(1, 1, 1, 1)).score()).isEqualTo(100);
        assertThat(TravelStyleMatch.compare(answers(1, 1, 1, 1), answers(5, 5, 5, 5)).score()).isZero();
    }

    @Test
    void neverImputesMissingOrInvalidAnswers() {
        assertThat(TravelStyleMatch.isComplete(Map.of("planning", 3))).isFalse();
        assertThat(TravelStyleMatch.isComplete(answers(0, 3, 3, 3))).isFalse();
        assertThatThrownBy(() -> TravelStyleMatch.compare(answers(3, 3, 3, 3), Map.of("planning", 3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private Map<String, Integer> answers(int planning, int pace, int together, int spending) {
        return Map.of("planning", planning, "daily-pace", pace, "time-together", together, "spending", spending);
    }
}
