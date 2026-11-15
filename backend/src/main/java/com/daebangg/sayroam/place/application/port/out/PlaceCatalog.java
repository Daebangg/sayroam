package com.daebangg.sayroam.place.application.port.out;

import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;

/** 여행에 담을 장소를 찾는 외부 장소 정보 공급자. */
public interface PlaceCatalog {

    PlaceSearchResult search(PlaceSearchQuery query);

}
