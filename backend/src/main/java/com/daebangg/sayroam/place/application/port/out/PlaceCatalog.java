package com.daebangg.sayroam.place.application.port.out;

import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.domain.Place;

import java.util.Optional;

/** 여행에 담을 장소를 찾는 외부 장소 정보 공급자. */
public interface PlaceCatalog {

    PlaceSearchResult search(PlaceSearchQuery query);

    /** 출처에 없거나 여행에 담을 수 없는 장소면 비어 있다. */
    Optional<Place> find(String sourceId);

}
