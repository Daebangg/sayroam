package com.daebangg.sayroam.place.domain;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlaceNameTest {

    @Test
    void 앞뒤_공백을_지운다() {
        // when
        PlaceName placeName = PlaceName.from("  메가커피 송정점 ");

        // then
        assertThat(placeName.value()).isEqualTo("메가커피 송정점");
    }

    @Test
    void 이름이_비어_있으면_거부한다() {
        // when & then
        assertThatThrownBy(() -> PlaceName.from(null))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_NAME_REQUIRED));
        assertThatThrownBy(() -> PlaceName.from("   "))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_NAME_REQUIRED));
    }

    @Test
    void 최대_길이를_넘으면_거부한다() {
        // given
        String tooLongName = "가".repeat(PlaceName.MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> PlaceName.from(tooLongName))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_NAME_TOO_LONG));
    }

}
