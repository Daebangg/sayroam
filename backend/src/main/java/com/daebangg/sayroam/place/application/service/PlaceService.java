package com.daebangg.sayroam.place.application.service;

import com.daebangg.sayroam.place.application.dto.PlaceSearchQuery;
import com.daebangg.sayroam.place.application.dto.PlaceSearchResult;
import com.daebangg.sayroam.place.application.port.out.PlaceCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaceService {
    private final PlaceCatalog placeCatalog;

    public PlaceSearchResult search(PlaceSearchQuery query) {
        return placeCatalog.search(query);
    }
}
