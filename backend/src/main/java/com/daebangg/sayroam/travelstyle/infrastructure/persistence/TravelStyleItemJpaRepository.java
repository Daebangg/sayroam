package com.daebangg.sayroam.travelstyle.infrastructure.persistence;

import com.daebangg.sayroam.travelstyle.domain.TravelStyleItem;
import com.daebangg.sayroam.travelstyle.domain.TravelStyleItemRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelStyleItemJpaRepository extends TravelStyleItemRepository, JpaRepository<TravelStyleItem, Long> {
}
