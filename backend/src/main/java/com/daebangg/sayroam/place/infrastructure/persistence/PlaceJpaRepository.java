package com.daebangg.sayroam.place.infrastructure.persistence;

import com.daebangg.sayroam.place.domain.Place;
import com.daebangg.sayroam.place.domain.PlaceRepository;
import com.daebangg.sayroam.place.domain.PlaceSource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceJpaRepository extends PlaceRepository, JpaRepository<Place, Long> {
    @Override
    <S extends Place> S save(S place);

    @Override
    Optional<Place> findBySourceAndSourceId(PlaceSource source, String sourceId);

    long countBySource(PlaceSource source);
}
