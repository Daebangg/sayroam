package com.daebangg.sayroam.place.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.global.exception.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceTest {
    private static final String SOURCE_ID = "126081";
    private static final PlaceName NAME = new PlaceName("송정해수욕장");
    private static final PlaceCategory CATEGORY = PlaceCategory.ATTRACTION;
    private static final String SOURCE_CATEGORY_CODE = "12";
    private static final String ADDRESS = "부산광역시 해운대구 송정해변로 62";
    private static final Coordinate COORDINATE = new Coordinate(new BigDecimal("35.1786"), new BigDecimal("129.1997"));

    @Test
    void 필수_값이_있으면_장소를_만들_수_있다() {
        // when & then
        assertThatCode(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, SOURCE_CATEGORY_CODE, ADDRESS, COORDINATE))
                .doesNotThrowAnyException();
    }

    @Test
    void 주소가_없어도_장소를_만들_수_있다() {
        // when & then
        assertThatCode(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, SOURCE_CATEGORY_CODE, null, COORDINATE))
                .doesNotThrowAnyException();
    }

    @Test
    void 이름이_없으면_장소를_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, null, CATEGORY, SOURCE_CATEGORY_CODE, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 분류가_없으면_장소를_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, NAME, null, SOURCE_CATEGORY_CODE, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 좌표가_없으면_장소를_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, SOURCE_CATEGORY_CODE, ADDRESS, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 원본_ID가_비어_있으면_장소를_만들_수_없다(String sourceId) {
        // when & then
        assertThatThrownBy(() -> Place.tour(sourceId, NAME, CATEGORY, SOURCE_CATEGORY_CODE, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 원본_ID가_최대_길이를_넘으면_장소를_만들_수_없다() {
        // given
        String sourceId = "1".repeat(Place.MAX_SOURCE_ID_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Place.tour(sourceId, NAME, CATEGORY, SOURCE_CATEGORY_CODE, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void 원본_분류_코드가_비어_있으면_장소를_만들_수_없다(String sourceCategoryCode) {
        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, sourceCategoryCode, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 원본_분류_코드가_최대_길이를_넘으면_장소를_만들_수_없다() {
        // given
        String sourceCategoryCode = "1".repeat(Place.MAX_SOURCE_CATEGORY_CODE_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, sourceCategoryCode, ADDRESS, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 주소가_최대_길이면_장소를_만들_수_있다() {
        // given
        String address = "가".repeat(Place.MAX_ADDRESS_LENGTH);

        // when & then
        assertThatCode(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, SOURCE_CATEGORY_CODE, address, COORDINATE))
                .doesNotThrowAnyException();
    }

    @Test
    void 주소가_최대_길이를_넘으면_장소를_만들_수_없다() {
        // given
        String address = "가".repeat(Place.MAX_ADDRESS_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Place.tour(SOURCE_ID, NAME, CATEGORY, SOURCE_CATEGORY_CODE, address, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 직접_추가하는_장소는_이름과_좌표만으로_만들_수_있다() {
        // when & then
        assertThatCode(() -> Place.custom(NAME, COORDINATE))
                .doesNotThrowAnyException();
    }

    @Test
    void 직접_추가하는_장소는_이름이_없으면_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> Place.custom(null, COORDINATE))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }

    @Test
    void 직접_추가하는_장소는_좌표가_없으면_만들_수_없다() {
        // when & then
        assertThatThrownBy(() -> Place.custom(NAME, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_INPUT));
    }
}
