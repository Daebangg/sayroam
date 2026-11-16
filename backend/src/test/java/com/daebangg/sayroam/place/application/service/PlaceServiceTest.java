package com.daebangg.sayroam.place.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import com.daebangg.sayroam.place.domain.Coordinate;
import com.daebangg.sayroam.place.domain.Place;
import com.daebangg.sayroam.place.domain.PlaceCategory;
import com.daebangg.sayroam.place.domain.PlaceName;
import com.daebangg.sayroam.place.domain.PlaceSource;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import com.daebangg.sayroam.place.infrastructure.persistence.PlaceJpaRepository;
import com.daebangg.sayroam.support.DatabaseIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PlaceServiceTest extends DatabaseIntegrationTest {
    private static final String SOURCE_ID = "126081";

    @Autowired
    PlaceService placeService;

    @Autowired
    PlaceJpaRepository placeJpaRepository;

    @MockitoBean
    PlaceCatalog placeCatalog;

    @AfterEach
    void cleanUp() {
        placeJpaRepository.deleteAllInBatch();
    }

    @Test
    void 처음_담는_장소는_상세_조회해_저장한다() {
        // given
        given(placeCatalog.find(SOURCE_ID)).willReturn(Optional.of(tourPlace(SOURCE_ID)));

        // when
        Long placeId = placeService.register(SOURCE_ID);

        // then
        assertThat(placeJpaRepository.findById(placeId)).isPresent();
        assertThat(placeJpaRepository.count()).isEqualTo(1);
    }

    @Test
    void 이미_저장된_장소는_다시_조회하지_않고_같은_장소를_준다() {
        // given
        Long savedId = placeJpaRepository.save(tourPlace(SOURCE_ID)).getId();

        // when
        Long placeId = placeService.register(SOURCE_ID);

        // then
        assertThat(placeId).isEqualTo(savedId);
        assertThat(placeJpaRepository.count()).isEqualTo(1);
        verify(placeCatalog, never()).find(any());
    }

    @Test
    void TourAPI에서_찾을_수_없는_장소면_PLACE_NOT_FOUND로_거부한다() {
        // given
        given(placeCatalog.find(SOURCE_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> placeService.register(SOURCE_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND));
        assertThat(placeJpaRepository.count()).isZero();
    }


    private Place tourPlace(String sourceId) {
        return new Place(PlaceSource.TOUR, sourceId, new PlaceName("송정해수욕장"),
                PlaceCategory.ATTRACTION, "12", "부산광역시 해운대구 송정해변로 62",
                new Coordinate(new BigDecimal("35.1786"), new BigDecimal("129.1997")));
    }
}