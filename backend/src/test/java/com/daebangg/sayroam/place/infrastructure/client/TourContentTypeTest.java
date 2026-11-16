package com.daebangg.sayroam.place.infrastructure.client;

import com.daebangg.sayroam.place.domain.PlaceCategory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TourContentTypeTest {

    @ParameterizedTest
    @CsvSource({
            "12, ATTRACTION",
            "14, CULTURE",
            "28, ACTIVITY",
            "32, LODGING",
            "38, SHOPPING",
            "39, RESTAURANT"
    })
    void TourAPI_코드를_서비스_분류로_바꾼다(String code, PlaceCategory expected) {
        // when
        Optional<PlaceCategory> category = TourContentType.categoryOf(code);

        // then
        assertThat(category).contains(expected);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"15", "25", "99", ""})
    void 축제_코스나_모르는_코드는_분류가_없다(String code) {
        // when
        Optional<PlaceCategory> category = TourContentType.categoryOf(code);

        // then
        assertThat(category).isEmpty();
    }

    @Test
    void 분류를_TourAPI_코드로_바꾼다() {
        // when
        String code = TourContentType.codeOf(PlaceCategory.LODGING);

        // then
        assertThat(code).isEqualTo("32");
    }

    @ParameterizedTest
    @EnumSource(PlaceCategory.class)
    void 모든_서비스_분류는_TourAPI_코드로_왕복된다(PlaceCategory category) {
        // when
        String code = TourContentType.codeOf(category);

        // then
        assertThat(TourContentType.categoryOf(code)).contains(category);
    }

}
