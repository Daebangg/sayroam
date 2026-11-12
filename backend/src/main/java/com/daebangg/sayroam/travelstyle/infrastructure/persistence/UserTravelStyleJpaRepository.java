package com.daebangg.sayroam.travelstyle.infrastructure.persistence;

import com.daebangg.sayroam.travelstyle.domain.UserTravelStyle;
import com.daebangg.sayroam.travelstyle.domain.UserTravelStyleRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTravelStyleJpaRepository extends UserTravelStyleRepository, JpaRepository<UserTravelStyle, Long> {
    @Override
    @EntityGraph(attributePaths = "item")
    List<UserTravelStyle> findByUserId(long userId);
}
