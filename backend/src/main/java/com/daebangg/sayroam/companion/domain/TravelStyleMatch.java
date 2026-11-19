package com.daebangg.sayroam.companion.domain;

import com.daebangg.sayroam.travelstyle.domain.TravelStyleItem;

import java.util.List;
import java.util.Map;

public record TravelStyleMatch(double score, Category category, List<Item> items) {
    public static final List<String> QUESTION_CODES = TravelStyleItem.MATCHING_CODES;

    public static boolean isComplete(Map<String, Integer> answers) {
        return QUESTION_CODES.stream().allMatch(code -> {
            Integer score = answers.get(code);
            return score != null && score >= 1 && score <= 5;
        });
    }

    public static TravelStyleMatch compare(Map<String, Integer> mine, Map<String, Integer> candidate) {
        if (!isComplete(mine) || !isComplete(candidate)) {
            throw new IllegalArgumentException("Four travel style answers are required");
        }
        List<Item> items = QUESTION_CODES.stream().map(code -> Item.compare(code, mine.get(code), candidate.get(code)))
                .toList();
        int totalDifference = items.stream().mapToInt(Item::difference).sum();
        Category category = items.stream().allMatch(item -> item.difference() <= 1)
                ? Category.SIMILAR : Category.NEEDS_DISCUSSION;
        return new TravelStyleMatch(100.0 * (1.0 - totalDifference / 16.0), category, items);
    }

    public enum Category { SIMILAR, NEEDS_DISCUSSION }
    public enum Similarity { SAME, SIMILAR, SOMEWHAT_DIFFERENT, DIFFERENT }

    public record Item(String code, int myScore, int candidateScore, int difference, Similarity similarity) {
        private static Item compare(String code, int mine, int candidate) {
            int difference = Math.abs(mine - candidate);
            Similarity similarity = switch (difference) {
                case 0 -> Similarity.SAME;
                case 1 -> Similarity.SIMILAR;
                case 2 -> Similarity.SOMEWHAT_DIFFERENT;
                default -> Similarity.DIFFERENT;
            };
            return new Item(code, mine, candidate, difference, similarity);
        }
    }
}
