package com.daebangg.sayroam.place.application.service;

import com.daebangg.sayroam.global.exception.BusinessException;
import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import com.daebangg.sayroam.place.domain.PlaceRepository;
import com.daebangg.sayroam.place.domain.PlaceSource;
import com.daebangg.sayroam.place.domain.error.PlaceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceCatalog placeCatalog;
    private final PlaceRepository placeRepository;

    public PlaceSearchResult search(PlaceSearchQuery query) {
        return placeCatalog.search(query);
    }

    @Transactional
    public Long register(String sourceId) {
        return placeRepository.findBySourceAndSourceId(PlaceSource.TOUR, sourceId)
                .orElseGet(() -> placeRepository.save(placeCatalog.find(sourceId)
                        .orElseThrow(() -> new BusinessException(PlaceErrorCode.PLACE_NOT_FOUND))))
                .getId();
    }
}
