package com.daebangg.sayroam.place.domain;

import java.util.Optional;

public interface PlaceRepository {
    <S extends Place> S save(S place);

    Optional<Place> findBySourceAndSourceId(PlaceSource source, String sourceId);
}
